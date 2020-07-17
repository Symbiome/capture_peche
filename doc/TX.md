# Transactions

Tous les services REST héritent de 
[AbstractFisholaResource](/fishola-backend/src/main/java/fr/inrae/fishola/rest/AbstractFisholaResource.java) qui a une
annotation `@Transactional(REQUIRED)`.

`REQUIRED` correspond à :

         If called outside a transaction context, the interceptor must begin a new
         Jakarta Transactions transaction, the managed bean method execution must then continue
         inside this transaction context, and the transaction must be completed by
         the interceptor.
         If called inside a transaction context, the managed bean
         method execution must then continue inside this transaction context.
        
Donc pour chaque appel entrant à un service REST une transaction est crée. Pour chaque appel réentrant, la transaction
est conservée.

Chaque DAO "métier" (par opposition aux DAOs "techniques" générés par JOOQ) hérite de [AbstractFisholaDao](/fishola-backend/src/main/java/fr/inrae/fishola/database/AbstractFisholaDao.java) qui a une annotation `@Transactional(MANDATORY)`.
`MANDATORY` correspond à :

         If called outside a transaction context, a TransactionalException with a
         nested TransactionRequiredException must be thrown.
         If called inside a transaction context, managed bean method execution will
         then continue under that context.

Donc pour tout appel entrant sur un DAO, la transaction est propagée depuis le service REST appelant, mais cette
transaction n'est jamais créée spécifique pour le DAO.

Le test `testTransaction` dans
[TripResourceTest](/fishola-backend/src/test/java/fr/inrae/fishola/rest/trips/TripResourceTest.java) vérifie le bon
fonctionnement du mécanisme transactionnel :

     Le test se base sur le fait que la création d'une sortie et de ses captures se fait en 2 temps : d'abord la
     création de la sortie, puis création des captures.
     Dans le test ci-dessous, la sortie est valide mais pas la capture, la création de la sortie en base est donc bien
     faite mais comme la création de la capture échoue, le rollback vient annuler la création de la sortie.
