import time

# Import SPI library (for hardware SPI) and MCP3008 library.
import Adafruit_GPIO.SPI as SPI
import Adafruit_MCP3008
import Adafruit_DHT
from mq import MQ

#software SPI
#CLK  = 18
#MISO = 23
#MOSI = 24
#CS   = 25

#sensor pin list
TEMP = 11
HUM = 4
GAS = 22
LIGHT = 21

# Hardware SPI configuration:
SPI_PORT   = 0
SPI_DEVICE = 0
#mcp = Adafruit_MCP3008.MCP3008(spi=SPI.SpiDev(SPI_PORT, SPI_DEVICE))

class Sensor():
    def __init__(self):
        mcp = Adafruit_MCP3008.MCP3008(spi=SPI.SpiDev(SPI_PORT, SPI_DEVICE))
        #mcp = Adafruit_MCP3008.MCP3008(clk=CLK, cs=CS, miso=MISO, mosi=MOSI)

    def read_sensor(sensor):
        temp, hum = read_temp_hum()
        if sensor == "TEMP":
            return temp
        if sensor == "HUM":
            return hum
        value = mcp.read_adc(sensor)
        volts = ConvertVolts(value,2)
        if sensor == "LIGHT":
            return volts
        if sensor == "GAS":
            mq = MQ()
            perc = mq.MQPercentage
            return (perc["GAS_LPG"], perc["CO"], perc["SMOKE"])

    def ConvertVolts(data,places):
        volts = (data * 3.3) / float(1023)
        volts = round(volts,places)
        return volts

    def read_temp_hum():
        return Adafruit_DHT.read_retry(TEMP, HUM)