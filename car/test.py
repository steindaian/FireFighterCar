#!/usr/bin/env python

import sys
import io
import os
import shutil
from subprocess import Popen, PIPE
from string import Template
from struct import Struct
from threading import Thread
from time import sleep, time
from http.server import HTTPServer, BaseHTTPRequestHandler
from wsgiref.simple_server import make_server

import picamera
from ws4py.websocket import WebSocket
from ws4py.server.wsgirefserver import WSGIServer, WebSocketWSGIRequestHandler
from ws4py.server.wsgiutils import WebSocketWSGIApplication

import serial
import struct

###########################################
# CONFIGURATION
WIDTH = 640
HEIGHT = 480
FRAMERATE = 24
HTTP_PORT = 8082
WS_PORT = 8084
COLOR = u'#444'
BGCOLOR = u'#333'
JSMPEG_MAGIC = b'jsmp'
JSMPEG_HEADER = Struct('>4sHH')
VFLIP = False
HFLIP = False

###########################################


class StreamingHttpHandler(BaseHTTPRequestHandler):
    def do_HEAD(self):
        self.do_GET()

    def do_GET(self):
        if self.path == '/':
            self.send_response(301)
            self.send_header('Location', '/index.html')
            self.end_headers()
            return
        elif self.path == '/jsmpg.js':
            content_type = 'application/javascript'
            content = self.server.jsmpg_content
        elif self.path == '/index.html':
            content_type = 'text/html; charset=utf-8'
            tpl = Template(self.server.index_template)
            content = tpl.safe_substitute(dict(
                WS_PORT=WS_PORT, WIDTH=WIDTH, HEIGHT=HEIGHT, COLOR=COLOR,
                BGCOLOR=BGCOLOR))
        else:
            self.send_error(404, 'File not found')
            return
        content = content.encode('utf-8')
        self.send_response(200)
        self.send_header('Content-Type', content_type)
        self.send_header('Content-Length', len(content))
        self.send_header('Last-Modified', self.date_time_string(time()))
        self.end_headers()
        if self.command == 'GET':
            self.wfile.write(content)


class StreamingHttpServer(HTTPServer):
    def __init__(self):
        super(StreamingHttpServer, self).__init__(
                ('', HTTP_PORT), StreamingHttpHandler)
        with io.open('index.html', 'r') as f:
            self.index_template = f.read()
        with io.open('jsmpg.js', 'r') as f:
            self.jsmpg_content = f.read()


class StreamingWebSocket(WebSocket):
    def opened(self):
        self.send(JSMPEG_HEADER.pack(JSMPEG_MAGIC, WIDTH, HEIGHT), binary=True)
		self.ser = serial.Serial ("/dev/ttyAMA0")    #Open named port 
		self.ser.baudrate = 9600 
		self.cmd=0
		
	def received_message(self,msg):
        msg = str(msg)
        cmd = self.cmd
        if msg == 'W1':
            if self.cmd in [0,2,7,8]:
                cmd = 1
            if self.cmd == 3:
                cmd = 5
            if self.cmd == 4:
                cmd = 6
        if msg == 'A1':
            if self.cmd in [0,6,4,8]:
                cmd = 3
            if self.cmd == 1:
                cmd = 5
            if self.cmd == 2:
                cmd = 7
        if msg == 'D1':
            if self.cmd in [0,3,5,7]:
                cmd = 4
            if self.cmd == 1:
                cmd = 6
            if self.cmd == 2:
                cmd = 8
        if msg == 'S1':
            if self.cmd in [0,1,5,6]:
                cmd = 2
            if self.cmd == 3:
                cmd = 7
            if self.cmd == 4:
                cmd = 8
        if msg == 'W0':
            if self.cmd == 5:
                cmd = 3
            if self.cmd == 6:
                cmd = 4
            if self.cmd == 1:
                cmd = 0
        if msg == 'A0':
            if self.cmd == 5:
                cmd = 1
            if self.cmd == 7:
                cmd = 2
            if self.cmd == 3:
                cmd = 0
        if msg == 'D0':
            if self.cmd == 6:
                cmd = 1
            if self.cmd == 8:
                cmd = 2
            if self.cmd == 4:
                cmd = 0
        if msg == 'S0':
            if self.cmd == 7:
                cmd = 3
            if self.cmd == 8:
                cmd = 4
            if self.cmd == 2:
                cmd = 0
        if self.cmd != cmd:
            self.ser.write(struct.pack('>B',cmd))
            print('Sent %d ' % cmd)
        self.cmd = cmd


class BroadcastOutput(object):
    def __init__(self, camera):
        print('Spawning background conversion process')
        self.converter = Popen([
            'avconv',
            '-f', 'rawvideo',
            '-pix_fmt', 'yuv420p',
            '-s', '%dx%d' % camera.resolution,
            '-r', str(float(camera.framerate)),
            '-i', '-',
            '-f', 'mpeg1video',
            '-b', '800k',
            '-r', str(float(camera.framerate)),
            '-'],
            stdin=PIPE, stdout=PIPE, stderr=io.open(os.devnull, 'wb'),
            shell=False, close_fds=True)

    def write(self, b):
        self.converter.stdin.write(b)

    def flush(self):
        print('Waiting for background conversion process to exit')
        self.converter.stdin.close()
        self.converter.wait()


class BroadcastThread(Thread):
    def __init__(self, converter, websocket_server):
        super(BroadcastThread, self).__init__()
        self.converter = converter
        self.websocket_server = websocket_server

    def run(self):
        try:
            while True:
                buf = self.converter.stdout.read(512)
                if buf:
                    self.websocket_server.manager.broadcast(buf, binary=True)
                elif self.converter.poll() is not None:
                    break
        finally:
            self.converter.stdout.close()


def main():
    print('Initializing camera')
    with picamera.PiCamera() as camera:
        camera.resolution = (WIDTH, HEIGHT)
        camera.framerate = FRAMERATE
        camera.vflip = VFLIP # flips image rightside up, as needed
        camera.hflip = HFLIP # flips image left-right, as needed
        sleep(1) # camera warm-up time
        print('Initializing websockets server on port %d' % WS_PORT)
        websocket_server = make_server(
            '', WS_PORT,
            server_class=WSGIServer,
            handler_class=WebSocketWSGIRequestHandler,
            app=WebSocketWSGIApplication(handler_cls=StreamingWebSocket))
        websocket_server.initialize_websockets_manager()
        websocket_thread = Thread(target=websocket_server.serve_forever)
        print('Initializing HTTP server on port %d' % HTTP_PORT)
        http_server = StreamingHttpServer()
        http_thread = Thread(target=http_server.serve_forever)
        print('Initializing broadcast thread')
        output = BroadcastOutput(camera)
        broadcast_thread = BroadcastThread(output.converter, websocket_server)
        print('Starting recording')
        camera.start_recording(output, 'yuv')
        try:
            print('Starting websockets thread')
            websocket_thread.start()
            print('Starting HTTP server thread')
            http_thread.start()
            print('Starting broadcast thread')
            broadcast_thread.start()
            while True:
                camera.wait_recording(1)
        except KeyboardInterrupt:
            pass
        finally:
            print('Stopping recording')
            camera.stop_recording()
            print('Waiting for broadcast thread to finish')
            broadcast_thread.join()
            print('Shutting down HTTP server')
            http_server.shutdown()
            print('Shutting down websockets server')
            websocket_server.shutdown()
            print('Waiting for HTTP server thread to finish')
            http_thread.join()
            print('Waiting for websockets thread to finish')
            websocket_thread.join()


if __name__ == '__main__':
    main()
