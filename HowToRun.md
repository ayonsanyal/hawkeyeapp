Hawk Eye App

This application is a reactive streaming application which streams new from
 flight Details from csv
 
 It takes care of the following scenario:
 
 1.The stats are displayed from all the sources if the source name is valid.
 2.The flight details are streamed continuously and relevant data is extracted from csv.
 
 
  
  
  This application is a scala app with following feature 
  1.Server which runs and up a web service and accepts the http  rest end points.
  
  
  Important point to note:
 
  
  The web service part is a akka http based restful service which fetches the news from 
from airport.csv on the basis with the help of it's endpoints.
  
  Hence ,the application has following parts
  
  1. Server which will up the restful service and it will respond with  the airport statistics  on request.
    
   2.Client which consumes this service via rest endpoints.
   
   
   Architecture:
   
   
   This project follows the micro service based architecture which is developed on the top 
   of akka(actor based model).
   
   1.The architecture follows a simple methodology which every service will follow:
    
    (i) The incoming request will be transferred to service manager actor .The service managers
        will delegate the request to corresponding child actors .
    (ii) The child actors will process the request and respond with corresponding output.
    (iii) The corresponding output will be treated as a response of the restful web service.
        
        
   Every service or module is independent except the common module.Every service will depend on common module 
   
   2.Every actors are responsible for single responsibility only.
   
  
   3.The only way to communicate with this service is restful end point.
   
   
   
   How to Run The App:
   
   1.Unzip the zip file .
   2. Import it into your preferred IDE(IntelliJIdea Recommended).
   3.From the terminal run sbt clean(Recommended).
   4. Run the scala singleton object Server  which in the Server Directory.
   
   
   Note: The server should be run before running the client,otherwise the client 
   will not able to show the results properly.
   
   To run the tests ,please follow the below instructions:
   
   1.From the root folder ,go to the command prompt or shell and execute
     sbt clean test
   
   Technology and frameworks used:
   
   1.Scala 2.12
   2.Akka 2.5.6
   3.Akka http
   4.Akka Streams
   5.Spray Json for marshalling the response
   
   
   
   
   Requirement:
   
   1. Java version more than 1.6
   2. Sbt
   
   References:
   
   1. Akka Http Docs -10.1.1.
   2. Akka HTTP client pooling and parallelism- Greg Beech
   
   
   