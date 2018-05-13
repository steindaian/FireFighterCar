#!/usr/bin/python3

from threading import Thread
import time
import RPi.GPIO as GPIO
import sys
from firebase import firebase

# Import SPI library (for hardware SPI) and MCP3008 library.
import Adafruit_GPIO.SPI as SPI
import Adafruit_MCP3008
import Adafruit_DHT
from mq import *
#software SPI
#CLK  = 18
#MISO = 23
#MOSI = 24
#CS   = 25



# Hardware SPI configuration:
#SPI_PORT   = 0
#SPI_DEVICE = 0
#mcp = Adafruit_MCP3008.MCP3008(spi=SPI.SpiDev(SPI_PORT, SPI_DEVICE))


class Sensor():

    #sensor pin list
    DHT11 = 4
    GAS_A = 0
    GAS_D = 37
    DIS_TRIG = 16
    DIS_ECHO = 18
    SOUND_SPEED_CONSTANT    = 17150

    FIREBASE_ROOT = "https://ms-proj.firebaseio.com/"

    def __init__(self):
        GPIO.setwarnings(False)
        GPIO.cleanup()
        GPIO.setmode(GPIO.BOARD)
        GPIO.setup(self.GAS_D,GPIO.IN,pull_up_down=GPIO.PUD_DOWN)
        GPIO.setup(self.DIS_TRIG,GPIO.OUT)
        GPIO.setup(self.DIS_ECHO,GPIO.IN)

        self.en = True
        self.en_distance = True
        self.en_temp = True
        self.en_smoke = True

        self.firebase = firebase.FirebaseApplication(self.FIREBASE_ROOT, None)

        self.distance = -1
        self.temperature = -1
        self.humidity = -1
        self.co_leak = False
        self.co_level = -1.0
        self.lpg = -1.0
        self.smoke = -1.0

    def run(self):
        #t1 = Thread(target = self.read_distance)
        #t2 = Thread(target = self.read_mq)
        t3 = Thread(target = self.read_temp_hum)
        try:
            #t1.start()
            #t2.start()
            t3.start()
            time.sleep(2)
            while self.en:
                data = {"temp": self.temperature, "humidity": self.humidity, "co_leak": self.co_leak,"lpg": self.lpg,"co_level": self.co_level,"smoke": self.smoke,"distance":self.distance}
                self.firebase.post('/sensor', data)
        except Exception as e:
            raise
        finally:
            time.sleep(0.5)
            #t1.join()
            #t2.join()
            t3.join()
                

    def read_distance(self):
        GPIO.output(self.DIS_TRIG,False)
        time.sleep(2)
        while self.en_distance:
            GPIO.output(self.DIS_TRIG, True)
            time.sleep(0.00001)
            GPIO.output(self.DIS_TRIG, False)

            while GPIO.input(self.DIS_ECHO)==0:
                pulse_start = time.time()
            while GPIO.input(self.DIS_ECHO)==1:
                pulse_end = time.time()    

            pulse_duration = pulse_end - pulse_start
            dist = pulse_duration * self.SOUND_SPEED_CONSTANT
            self.distance = round(dist,2)
            time.sleep(2)
            #return round(dist,2)


    def read_mq(self):
        #prepare mq-7 sensor
        self.mq = MQ(10,self.GAS_A)
        while self.en_smoke:
            #10,analog pin
            self.co_leak = False
            #read
            perc = self.mq.MQPercentage()
            self.co_level = perc["CO"]
            self.lpg = perc["GAS_LPG"]
            self.smoke = perc["SMOKE"]
            #sys.stdout.write("LPG: %g ppm, CO: %g ppm, Smoke: %g ppm\n" % (perc["GAS_LPG"], perc["CO"], perc["SMOKE"]))
            #sys.stdout.flush()
            if GPIO.input(self.GAS_D) == 0:
                self.co_leak = True
                #print("CO leak detected")
            #return (co_leak,perc["GAS_LPG"], perc["CO"], perc["SMOKE"])
        

    def read_temp_hum(self):
        while self.en_temp:
            #read_retry(sensor,pin),sensor==11
            self.humidity, self.temperature = Adafruit_DHT.read_retry(11, self.DHT11)
            #print ('Temp: {0:0.1f} C  Humidity: {1:0.1f} %'.format(self.temperature, self.humidity))
            #return Adafruit_DHT.read_retry(11, self.DHT11)

    def stop(self):
        print('Stopping the sensors')
        self.en_distance = False
        time.sleep(0.5)
        self.en_smoke = False
        time.sleep(0.5)
        self.en_temp = False
        time.sleep(0.5)
        self.en = False
        time.sleep(0.5)
        GPIO.cleanup()

if __name__ == '__main__':
    s = Sensor()
    t = Thread(target = s.run)
    try:
        t.start()
        time.sleep(100)
    except Exception as e:
        pass
    finally:
        s.stop()
        t.join()
    