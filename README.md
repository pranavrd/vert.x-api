# vert.x-api

a simple IoT Data Exchange application using Rest API backend using the Vert.x framework

  

## Highlights

 - RESTful CRUD APIs
 - MongoDB Client for Vert.x
 - Basic Auth Layer for all routes
 - Different Verticles for HTTP requests and DB access

### GET, PUT and DELETE
These endpoints route to ```http://localhost:8881/server/:id```
 **PUT** requires additional info in the ```http request``` body, namely ``` currentLevel```, ```observationDateTime```, 	```measuredDistance```, and ```referenceLevel```

### POST
This endpoint routes to ```http://localhost:8881/server/```
It also requires ``` currentLevel```, ```observationDateTime```, 	```measuredDistance```, and ```referenceLevel``` in the http request body.

> All endpoint require an AUTH_TOKEN header 


## How to Deploy 

 1. Clone [this](https://github.com/pranavrd/vert.x-api.git) repo 
 2. Run mongo and execute following steps :
	 i. ```use iot-data```
	 ii. ```db.coll.insert(sample_data)```  //*sample_data should be in json*
	 If you don't have mongo, you can install it from the [official page](https://www.mongodb.com/try/download/community)
3. Run the ```com.dataexchange.vertx.App``` instance in an IDE of your choice
4. Make calls to the endpoints from postman. [Here](https://www.getpostman.com/collections/a6d7d9bf8d17807b8165)'s a sample collection to get you started