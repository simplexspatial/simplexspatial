# Simple X Spatial architecture.

## Abstract Data Types (ADT)
Because the architecture is base in reactive oriented messages
patterns, the way to define the system is using Abstract Data Types
(ADT).

Don't confuse [Abstract Data
Types](https://en.wikipedia.org/wiki/Abstract_data_type) with the [Data
Transfer Object](https://en.wikipedia.org/wiki/Data_transfer_object).

In the ADT, the model is defined by the semantic. So using the model we
can know the behavior of the system.

So every actor is going to define their own data types in the companion
object and we should to use it via full name `{Class name}.{type}`
instead of import all types via import `{Class name}._`

When we say all messages, we are talking about request and response
messages. In the nearest future, we will start to use Event Sourcing and
Command Query Responsibility Segregation (CQRS) patterns.

To avoid the use of foreign messages in an Actor, we are going to use
the
[Adapted Response pattern](https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#adapted-response)
pattern intensely. [Here a good example](https://manuel.bernhardt.io/2019/08/07/tour-of-akka-typed-message-adapters-ask-pattern-and-actor-discovery/) as well.

## Spatial Data Model
Because the target of the server is to index spatial data, we need to
model this kind of data.

We want to model using `case classes` to take advance of all out of the
box features, but we don't want to implement (al least in the first
state of the project) all spatial algebra. So we will go with a model in
Scala and a wrapper to [JTS](https://github.com/locationtech/jts) model,
to be able to reuse all algebra implemented in this project.

More information about JTS:
- https://locationtech.github.io/jts/
- https://locationtech.github.io/jts/jts-features.html
- https://github.com/locationtech/jts
- http://docs.geotools.org/latest/userguide/
- http://docs.geotools.org/latest/userguide/library/jts/index.html
