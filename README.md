# Box Namer

Box Namer is a small application designed to keep track of hostnames of machines in server clusters.

Assigned hostnames take the form "_service_|_integer_" (e.g. `api34`). Your new machine can request
its name on startup by passing a service name to Box Namer's REST API. Box Namer will assign the box a name
by appending the lowest possible unused positive integer for the service to the service name.

For example, say we have already registered the following hostnames with Box Namer:

    api1
    api2
    api4
    api6

If we register for a new name for the `api` service, Box Namer will reserve the name `api3`.

## API Routes

All API routes currently expect JSON bodies with a content-type header of "application/json".

Errors will be returned as a JSON object:

    400 BAD REQUEST

    { "message": "Missing field 'basename'" }

#### Registering a New Name

    GET /api/v1/hostnames

Params: `basename`:  Required. An integer will be concatenated onto the basename to form the hostname.

Example response:

    {
      "name": "api34"
      "basename": "api"
      "index": 34
    }

#### Deregistering a Name

    DELETE /api/v1/hostnames/api34

## Data Persistence

Box Namer will periodically flush its registered names to disk so that it can recover them after a restart.
This writing is *not* guaranteed to complete successfully in the event of catastrophic failure. If you need
that type of assurance, it would not be difficult to modify the code to force a flush to disk after every
name change, but that would incur a performance penalty.

## Usage

Run the application in development mode via the startup script:

    ./bin/run_app.sh

To compile and run the application from a single jar file in production, you can use lein to create an uberjar:

    lein uberjar
    RING_ENV=production java -jar target/box-namer-0.1.0-SNAPSHOT-standalone.jar

