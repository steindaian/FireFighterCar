import RPi.GPIO as GPIO
import socket
import sys
import json
import time
from thread import Thread
from motor import Motor
from sensor import Sensor

class Stream(Thread):
    def __init__(self,server_adress,car):
        super(Stream,self).__init__()
        self.server_adress = server_adress
        self.car = car

    def run():
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.bind(self.server_address)
        sock.listen(1)

        my_sensor = Sensor()
        
        while True:
            connection, client_address = sock.accept()

            buff = b''
            try:
                print ('{} connected to {}'.format(time.strftime("%H:%M:%S"), client_address))
                while True:
                    # receive data in chunks
                    data = connection.recv(8)
                    if data:
                        buff += data
                    else:
                        break
                # parse received data
                json_recv = json.loads(buff.decode('utf-8'))
                try:
                    operation = json_recv['operation']
                    if operation == "read_sensor":
                        sensor = json_recv['sensor']
                        obj = { sensor: sensor, value: my_sensor.read_sensor(sensor) }
                        enc_obj = json.dumps(obj)
                        connection.send(enc_obj)

                    elif operation == "motor":
                        direction = json_recv['direction']
                        car.set_direction(direction)

                except Exception as e:
                    print('Something went wrong when running')
                finally:
                    pass
            
            finally:
                print ('{} disconnected from {}'.format(time.strftime("%H:%M:%S"), client_address))
                connection.close()

if __name__ == '__main__':
    server_address = ('localhost', 1221)
    c = Motor()
    s = Stream(server_address,c)
    s.run()