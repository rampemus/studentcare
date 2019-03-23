# Instructions

 - See general SBT project instructions here https://gitlab.utu.fi/tech/education/gui/templates-javafx-scene

## Installation

$ git clone https://gitlab.utu.fi/tech/education/gui/studentcare

$ cd studentcare

## Demo application

$ sbt run

## APIDoc

The project comes with a Doxygen configuration. Just run doxygen in the
project root folder:

$ doxygen

Or visit:
http://users.utu.fi/jmjmak/help-studentcare/

## Create an executable jar package for distribution

You can compile the project with

$ sbt assembly

The command generates target/studentcare-assembly-1.0.jar.
You can launch the jar with just

$ java -jar target/studentcare-assembly-1.0.jar
