
# MISE EN PLACE D’UNE APPLICATION MICROSERVICE DE SERVICE BANCAIRE( ORANGE – MTN )
          ## BankingSystemMaster

SITUATION PROBLEME:

Dans une plateforme, des utilisateurs aimeraient souscrire a des services financiers tels que les depots d’argent , les retraits d’argent, etc.. pour cela, on leur a demande d’addresser une demande de creation de compte dans une agence a laquelle ils sont affilies. De ce fait , la creation d’un utilisateur sera une demande envoye dans une agence, cette agence devra retransferer la dite demande dans le service approprie afin de verifier les informations qui sont dans la demande… le service aura le choix de valider la demande ou de la decliner :

– en cas de validation, le service qui  a valide la demande, enverra la reponse a l’agence en question, qui en suite se chargera de créer le compte de l’utilisateur correspondant. Le compte cree , l’utilisateur sera informe de cela a travers un service d’annonce.

– en cas de declinaison, le service qui  a refuse la demande, envoyera la reponse a l’agence en question, qui en suite se chargera tranferer la reponse negative a l’utilisateur correspondant avec le motif de reuf. l’utilisateur sera informe de cela a travers un service d’annonce.

TECHNOLOGIES DE BASES:

* Communication:  Architecture pilote par evenement cas de rabbitMq 
* Base de donnee evenementielle: cas de  EventStore
* Base de donne relationelle : H2 ou Mysql
* architecture d’implementation : Spring cloud


SERVICE DE BASES:
* service de configuration ( service-config) 
* service de registration ( service-registry) 
* service d’equilibrage de charge ( service-gateway) 
* service de tolerance de panne ( service-breaker) 

SCENARIO:

* Un utilisateur soumet une demande de création de compte à son agence. 
* L'agence transmet cette demande à un service dédié pour vérification. 
* Le service de vérification valide ou refuse la demande. 
* L'agence informe l'utilisateur du résultat. 

## SERVICES DE L’APPLICATION ( Fonctionnalites)

## Service d'agence: 
* Reçoit les demandes de création de compte.
* Transmet les demandes au service de vérification. 
* Reçoit les réponses du service de vérification. 
* Cree les comptes des utilisateurs dont les demandes sont valides
* Informe les utilisateurs du résultat. 
## Service de vérification: 
* Reçoit les demandes de création de compte. 
* Vérifie les informations fournies dans la demande. 
* Valide ou refuse la demande. 
* Envoie une réponse à l'agence. 
 ## Service de notification: 
* Recoit les reponses des demandes des utilisateurs
* Envoie des notifications aux utilisateurs (email et message broker).

 ## Service de portail Utilisateur 
* Soumission des demandes de création de compte. 
* Reception des notifications liées à son compte. 
* Sous-services possibles: 
* Authentification: Gestion des identifiants et des mots de passe. 
* Autorisation: Définition des droits d'accès de chaque utilisateur.
* retrait, depot et transfert
## Service de Transaction
  ### Sous-services: 
* Dépôt: Permet à l'utilisateur de créditer son compte. 
* Retrait: Permet à l'utilisateur de débiter son compte. 
* Transfert: Permet à l'utilisateur d'effectuer des transferts entre ses comptes ou vers d'autres comptes. 
* Historique: Stocke l'historique des transactions de l'utilisateur.




