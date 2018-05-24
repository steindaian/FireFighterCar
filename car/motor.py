import RPi.GPIO as GPIO
import socket
import sys
import json
import time



class Motor():

    def __init__(self):

        self.FREQ = 20000
        self.M1_EN1 = 8
        self.M1_EN2 = 10
        self.M1_PWM = 12

        self.M2_EN1 = 11
        self.M2_EN2 = 13
        self.M2_PWM = 15

        GPIO.setmode(GPIO.BOARD)
        GPIO.setwarnings(False)
        GPIO.setup(self.M1_PWM,GPIO.OUT)
        GPIO.setup(self.M2_PWM,GPIO.OUT)
        self.pwm1 = GPIO.PWM(self.M1_PWM,self.FREQ)
        self.pwm2 = GPIO.PWM(self.M2_PWM,self.FREQ)
        self.pwm1.start(0)
        self.pwm2.start(0)
        GPIO.setup(self.M1_EN1,GPIO.OUT)
        GPIO.setup(self.M1_EN2,GPIO.OUT) 
        GPIO.setup(self.M2_EN1,GPIO.OUT) 
        GPIO.setup(self.M2_EN2,GPIO.OUT) 
        print('Initializing motors at 20 KHz')

    def translate(value, leftMin, leftMax, rightMin, rightMax):
        # Figure out how 'wide' each range is
        leftSpan = leftMax - leftMin
        rightSpan = rightMax - rightMin

        # Convert the left range into a 0-1 range (float)
        valueScaled = float(value - leftMin) / float(leftSpan)

        # Convert the 0-1 range into a value in the right range.
        return rightMin + (valueScaled * rightSpan)

    def setSpeed(self,direction,speed):
        if speed != 0:
            speed = translate(speed,1,100,40,90)
        if 0 <= speed <= 100:
            self.pwm1.ChangeDutyCycle(speed)
            if direction:
                GPIO.output(self.M1_EN1,GPIO.HIGH)
                GPIO.output(self.M1_EN2,GPIO.LOW)
                print('speed: ' + str(speed) + ' forward')
            else:
                GPIO.output(self.M1_EN2,GPIO.HIGH)
                GPIO.output(self.M1_EN1,GPIO.LOW)
                print('speed: ' + str(speed) + ' backward')
    
    def setSteering(self,direction,steering):
        if steering > 0:
            steering = 100
        if steering < 0:
            steering = 0
        if 0 <= steering <= 100:
            self.pwm2.ChangeDutyCycle(steering)
            if direction:
                GPIO.output(self.M2_EN1,GPIO.HIGH)
                GPIO.output(self.M2_EN2,GPIO.LOW)
                print('steering:' + str(steering) + ' left')
            else:
                #print(self.M2_EN2)
                GPIO.output(self.M2_EN2,GPIO.HIGH)
                GPIO.output(self.M2_EN1,GPIO.LOW)
                print('steering: ' + str(steering) + ' right')

    def brake(self):
        self.pwm1.ChangeDutyCycle(0)
        self.pwm2.ChangeDutyCycle(0)
        
    def stop(self):
        self.pwm1.ChangeDutyCycle(0)
        self.pwm2.ChangeDutyCycle(0)
        print('Stopping motors')
        GPIO.cleanup()



if __name__ == '__main__':
    m = Motor()
    m.setSteering(0,100)
    m.setSpeed(True,50)
    time.sleep(300)
    m.stop()