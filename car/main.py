import RPi.GPIO as GPIO
import socket
import sys
import json
import time
from thread import Thread
from motor import Motor
from camera import Camera
from stream import MyStream

def main(server_address):
    car = Motor()
    car_thread = Thread(target=car.run)

    my_camera = Camera()
    camera_thread = Thread(target=my_camera.run)

    my_stream = MyStream(server_adress,car)
    stream_thread = Thread(target=my_stream.run)

    try:
        car_thread.start()
        camera_thread.start()
        stream_thread.start()
        while True:
            time.sleep(1)

    except Exception as e:
        pass
    finally:
        print('Stopping the car')
        car.stop()
        car_thread.join()
        camera_thread.join()
        stream_thread.join()

if __name__ == '__main__':
    server_address = ('localhost', 1221)
    main(server_address)