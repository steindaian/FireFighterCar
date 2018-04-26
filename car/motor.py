import RPi.GPIO as GPIO
import socket
import sys
import json
import time
from thread import Thread

FREQ 20000
M1_EN1 = 8
M1_EN2 = 10
M1_PWM = 12

M2_EN1 = 11
M2_EN2 = 13
M2_PWM = 15

class Motor(Thread):

    command = "stop"
    en = True
    m1_speed = 0
    m2_speed = 0

    def __init__(self):
        GPIO.set_mode(GPIO.BORD)
        self.pwm1 = GPIO.PWM(M1_PWM,FREQ)
        self.pwm2 = GPIO.PWM(M2_PW,FREQ)
        pwm1.start(0)
        pwm2.start(0)
        GPIO.setup(M1_EN1,GPIO.OUT)
        GPIO.setup(M1_EN2,GPIO.OUT) 
        GPIO.setup(M2_EN1,GPIO.OUT) 
        GPIO.setup(M2_EN2,GPIO.OUT) 

    def go_forward():
        GPIO.output(M1_EN1,1)
        GPIO.output(M1_EN2,0)
        GPIO.output(M1_EN1,1)
        GPIO.output(M2_EN2,0)

    def go_back():
        GPIO.output(M1_EN1,0)
        GPIO.output(M1_EN2,1)
        GPIO.output(M1_EN1,0)
        GPIO.output(M2_EN2,1)

    def check_speed():
        if m1_speed < 10:
            m1_speed = 0
        elif m1_speed > 100:
            m1_speed = 100
        if m2_speed < 10:
            m2_speed = 0
        elif m2_speed > 100:
            m2_speed = 100

    def set_cmd(cmd):
        self.command = cmd

    def set_speed():
        self.pwm1.ChangeDutyCycle(m1_speed)
        self.pwm2.ChangeDutyCycle(m2_speed)

    def stop():
        en = False
        
    def run():
        print('Starting car....')
        prev_cmd = self.command


        while en:
            print('Current command {}' % self.command)
            if self.command == "stop":
                m1_speed-=1
                m2_speed-=1
            elif self.command == "forward":
                if prev_cmd == "forward":
                    m1_speed+=1
                    m2_speed+=1
                else:
                    go_forward()
                    m1_speed = 10
                    m2_speed = 10
            elif self.command == "back":
                if prev_cmd == "back":
                    m1_speed+=1
                    m2_speed+=1
                else:
                    go_back()
                    m1_speed = 10
                    m2_speed = 10
            elif self.command == "left":
                if prev_cmd == "left":
                    m1_speed+=1
                    m2_speed+=1
                else:
                    m1_speed = 10
                    m2_speed = 15
            elif self.command == "right":
                if prev_cmd == "right":
                    m1_speed+=1
                    m2_speed+=1
                else:
                    m1_speed = 15
                    m2_speed = 10

            check_speed()
            set_speed()
            prev_cmd = self.cmd
            time.sleep(10)
        #stop motors
        pwm1.ChangeDutyCycle(0)
        pwm2.ChangeDutyCycle(0)
        GPIO.clean_up()



if __name__ == '__main__':
    m = Motor()
    m.run()