import RPi.GPIO as GPIO
import socket
import sys
import json
import time
from motor import Motor
from sensor import Sensor
import subprocess
from threading import Thread

def main(server_address):
    GPIO.setmode(GPIO.BOARD)
    car = Motor()
    print('Car motors system started')
    sensor = Sensor()
    sensor_thread = Thread(target = sensor.run)

    start_camera = "sudo systemctl start motion.service"
    stop_camera = "sudo systemctl stop motion.service"

    connection = None

    try:
        p = subprocess.call(start_camera.split())
        print('Camera feed started')
        
        sensor_thread.start()
        print("Sensor system started")

        print('Socket communication started')
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.bind(server_address)
        sock.listen(1)

        while True:
            print("Listening for client on port {}".format(server_address[1]))
            connection, client_address = sock.accept()
            en = True
            try:
                print ('{} connected to {}'.format(time.strftime("%H:%M:%S"), client_address))
                while en:
                    buff = b''
                    while True:
                        # receive data in chunks
                        data = connection.recv(1)
                        if data == b'':
                            en = False
                            break
                        if data != b'\n':
                            #print (repr(data))
                            buff += data
                        else:
                            #en = False
                            break
                    
                    #print("daian\n")
                    #buff = str(buff).strip('\n')
                    if buff == b'':
                        continue
                    print (repr(buff))
                    # parse received data
                    try:
                        
                        json_recv = json.loads(buff.decode('utf-8'))
                        operation = json_recv['operation']
                        if operation == "set_speed":
                            speed = json_recv['speed']
                            direction = json_recv['direction']
                            car.setSpeed(direction,speed)

                        elif operation == "set_steering":
                            direction = json_recv['direction']
                            steering = json_recv['steering']
                            car.setSteering(direction,steering)
                        elif operation == "stop":
                            car.brake()
                        else :
                            print(json.dumps(json_recv, indent=4, sort_keys=True))

                    except KeyError as e:
                        print('Some commands were missing from JSON')
                        pass
                        
                    except Exception as e:
                        print('Something went wrong when running(json didnt decode properly.Disconnecting...')
                        car.brake()
                        en = True
                        raise
                    finally:
                        pass
                        #en = False
            
            finally:
                print ('{} disconnected from {}'.format(time.strftime("%H:%M:%S"), client_address))
                connection.close()
                car.brake()
                en = True
                pass

    except Exception as e:
        print('Exception occured in starting functionalities')
        connection.close()
        raise
    finally:
        print('Stopping the car')
        car.brake()
        print('Stopping the socket communication')
        print("Stopping sensor system")
        sensor.stop()
        sensor_thread.join()
        print('Stopping the camera feed')
        q = subprocess.call(stop_camera.split())

if __name__ == '__main__':
    server_address = ('0.0.0.0', 8089)
    try:
        main(server_address)
    except KeyboardInterrupt as e:
        raise
    finally:
        time.sleep(2)
        GPIO.cleanup()
        pass
    