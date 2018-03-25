# Jenkins Pipeline - Jira - Lié un Job Jenkins à une tache Jira

Dans une optique d'intégration continue, l'objectif est de créer un item (ou job) Jenkins qui fait passer les tests d'intégration sur des branches d'un repository (Git) quand le ticket Jira est en statut merging. On va construire un item Jenkins de type Pipeline (Jenkins 2 via le language Groovy) avec le plugin Jira installé.

Pour plus d'info: http://www.la-pause-dev.fr/archives/10
