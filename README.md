CsrfRestExample
===============
Example how to secure scalable Spring Boot REST application against CSRF without using session.

Anti-CSRF tokens are stored in a Redis database to support multiple instances of the application. Spring security is instructed to use this TokenRepository
 instead of default HttpSessionCsrfTokenRepository.

GET /info
---------
unauthorized access to info resoucre

GET /login
----------
authorized resource that returns also csrf_token token in headers

PUT /info
---------
authorized access to resource that can be accessed only with correct csrf_token
