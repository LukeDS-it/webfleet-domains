# webfleet-domains
![build](https://github.com/LukeDS-it/webfleet-domains/workflows/build/badge.svg)
![Docker Image Version (latest by date)](https://img.shields.io/docker/v/ldsoftware/webfleet-domains)
![GitHub last commit](https://img.shields.io/github/last-commit/LukeDS-it/webfleet-domains)

This microservice is part of Webfleet, a new distributed cms.

# Building

To build, run `sbt build`

# Running locally

To run locally, first of all start the `docker-compose.yml` in the `environment` directory,
it will provide all the needed dependencies.

You will then need to set the correct environment variables, and then run `sbt run`

# Running on Docker

Docker version is available: https://hub.docker.com/repository/docker/ldsoftware/webfleet-domains
Setup the environment variables and you're good to go.

# Environment

The following are the environment variables that can be set or overridden

|      Variable     |                   Default                   |                                    Description                                          |
|-------------------|---------------------------------------------|-----------------------------------------------------------------------------------------|
| SERVER_PORT       | 8080                                        | HTTP Port where the application is exposed                                              |
| JDBC_DATABASE_URL | jdbc:postgresql://localhost:5432/webfleet   | Full JDBC url for the postgresql database for akka persistence                          |
| DATABASE_USER     | webfleet                                    | Username to connect to the DB                                                           |
| DATABASE_PASS     | password                                    | Password to connect to the DB                                                           |
| AUTH_DOMAIN       |                                             | Domain of Auth0 compliant provider. Used to look for $AUTH_DOMAIN/.well-known/jwks.json |
| AUTH_AUDIENCE     |                                             | Audience to validate the jwt token                                                      |
| AUTH_ISSUER       |                                             | Issuer to validate the jwt token                                                        |
| AMQP_URL          |                                             | URL of the rabbitmq instance in the form amqp://url.                                    |
| CLOUDAMQP_URL     |                                             | Alias for the AMQP_URL environment variable. For HEROKU compatibility                   |
| EXCHANGE_NAME     | webfleet                                    | Name of the message exchange that listeners will bind their queues to get domain events |
| DOMAINS_CHANNEL   | webfleet-domains                            | Channel name to use as routing key. Domain events will be tagged with this value        | 
