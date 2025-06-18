# 📚 Guide Complet d'Intégration - ServiceDemande

## 🎯 Vue d'Ensemble

Le **ServiceDemande** est le superviseur métier bancaire qui analyse, valide et supervise toutes les demandes de création de compte. Il fournit un dashboard complet pour la supervision des risques et la gestion des révisions manuelles.

### 🏗️ Architecture
- **Framework** : Spring Boot 3.x
- **Base de données** : MongoDB
- **Messaging** : RabbitMQ
- **Sécurité** : Spring Security (Basic Auth)
- **Documentation** : Swagger/OpenAPI 3.0

---

## 🌐 Configuration Base

### URL de Base
```
Développement : http://localhost:8081
Production : https://api-demande.production.com
```

### Headers Requis
```http
Content-Type: application/json
Authorization: Basic {base64(username:password)}
Accept: application/json
```

---

## 🔐 Authentification & Sécurité

### Comptes Disponibles
- **Supervisor** : `supervisor / supervisor123`
- **Admin** : `admin / admin123`

### Rôles et Permissions
- **SUPERVISOR** : Révision manuelle, consultation des demandes
- **ADMIN** : Toutes les fonctionnalités + modification des limites

### Authentification Basic Auth
```javascript
// Génération du header d'authentification
const username = 'supervisor';
const password = 'supervisor123';
const credentials = btoa(`${username}:${password}`);
const authHeader = `Basic ${credentials}`;

// Utilisation dans les requêtes
fetch('/api/v1/demande/dashboard', {
  headers: {
    'Authorization': authHeader,
    'Content-Type': 'application/json'
  }
});
```

---

## 📋 API Endpoints Complets

### 📊 **1. Dashboard et Statistiques**

#### Dashboard Principal
```http
GET /api/v1/demande/dashboard
Authorization: Basic {credentials}
Roles: ADMIN, SUPERVISOR
```

**Réponse :**
```json
{
  "totalDemandes": 1247,
  "demandesEnAttente": 23,
  "demandesEnAnalyse": 8,
  "demandesApprouvees": 1089,
  "demandesRejetees": 127,
  "revisionManuelleRequise": 15,
  "risqueFaible": 756,
  "risqueMoyen": 324,
  "risqueEleve": 134,
  "risqueCritique": 33,
  "demandesRecentes": 47,
  "tauxApprobation": 87.5,
  "scoreRisqueMoyen": 32.4,
  "generatedAt": "2024-06-18T15:30:00",
  "pourcentageApprobation": 89.6,
  "pourcentageRisqueEleve": 13.4
}
```

### 👨‍💼 **2. Révision Manuelle**

#### Liste des Demandes en Attente
```http
GET /api/v1/demande/manual-review/pending
Authorization: Basic {credentials}
Roles: SUPERVISOR, ADMIN
```

**Réponse :**
```json
[
  {
    "id": "demande_12345",
    "eventId": "evt_67890",
    "idClient": "CLI_001",
    "idAgence": "AG_YAO_001",
    "cni": "123456789012",
    "email": "client@example.com",
    "nom": "NGAMBA",
    "prenom": "Jean",
    "numero": "695123456",
    "status": "MANUAL_REVIEW",
    "riskScore": 65,
    "riskLevel": "HIGH",
    "fraudFlags": ["EMAIL_MULTIPLE_DEMANDES", "DEMANDES_FREQUENTES"],
    "requiresManualReview": true,
    "createdAt": "2024-06-18T10:30:00",
    "analyzedAt": "2024-06-18T10:35:00",
    "actionHistory": [
      {
        "actionType": "VALIDATION_REQUESTED",
        "description": "Demande reçue de ServiceAgence",
        "performedBy": "SYSTEM",
        "timestamp": "2024-06-18T10:30:00"
      },
      {
        "actionType": "FRAUD_ANALYSIS_COMPLETED",
        "description": "Analyse terminée - Score: 65, Niveau: HIGH",
        "performedBy": "ANTIFRAUD_SYSTEM",
        "timestamp": "2024-06-18T10:35:00"
      }
    ]
  }
]
```

#### Traiter une Révision Manuelle
```http
POST /api/v1/demande/manual-review/{demandeId}
Authorization: Basic {credentials}
Roles: SUPERVISOR, ADMIN
Content-Type: application/json

{
  "approved": true,
  "notes": "Profil client vérifié, documents authentifiés. Approbation accordée avec limites réduites.",
  "reviewerId": "SUPERVISOR_001"
}
```

**Réponse Succès :**
```json
{
  "status": "SUCCESS",
  "message": "Révision traitée avec succès",
  "demandeId": "demande_12345"
}
```

### 🔍 **3. Recherche et Consultation**

#### Recherche Avancée
```http
GET /api/v1/demande/search?status=APPROVED&riskLevel=HIGH&page=0&size=20&sortBy=createdAt&sortDir=desc
Authorization: Basic {credentials}
Roles: ADMIN, SUPERVISOR
```

**Paramètres de recherche :**
- `status` : RECEIVED, ANALYZING, MANUAL_REVIEW, APPROVED, REJECTED, EXPIRED
- `riskLevel` : LOW, MEDIUM, HIGH, CRITICAL
- `idAgence` : Filtrer par agence
- `idClient` : Filtrer par client
- `page` : Numéro de page (défaut: 0)
- `size` : Taille de page (défaut: 20)
- `sortBy` : Champ de tri (défaut: createdAt)
- `sortDir` : Direction (asc/desc, défaut: desc)

**Réponse :**
```json
{
  "content": [
    {
      "id": "demande_12345",
      "idClient": "CLI_001",
      "status": "APPROVED",
      "riskScore": 78,
      "riskLevel": "HIGH",
      "limiteDailyWithdrawal": "400000.00",
      "limiteDailyTransfer": "800000.00",
      "limiteMonthlyOperations": "5000000.00",
      "approvedAt": "2024-06-18T11:45:00"
    }
  ],
  "pageable": {
    "page": 0,
    "size": 20,
    "sort": ["createdAt,desc"]
  },
  "totalElements": 156,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

#### Détails d'une Demande
```http
GET /api/v1/demande/{demandeId}
Authorization: Basic {credentials}
Roles: ADMIN, SUPERVISOR
```

###  **4. Gestion des Limites**

#### Mise à Jour des Limites
```http
PUT /api/v1/demande/{demandeId}/limits
Authorization: Basic {credentials}
Roles: ADMIN
Content-Type: application/json

{
  "dailyWithdrawal": "500000.00",
  "dailyTransfer": "1000000.00",
  "monthlyOperations": "8000000.00"
}
```

###  **5. Supervision des Risques**

#### Comptes à Risque Élevé
```http
GET /api/v1/demande/high-risk?minRiskScore=70
Authorization: Basic {credentials}
Roles: ADMIN, SUPERVISOR
```

###  **6. Système**

#### Santé du Service
```http
GET /api/v1/demande/health
```

**Réponse :**
```json
{
  "status": "UP",
  "service": "ServiceDemande",
  "version": "2.0.0",
  "timestamp": "2024-06-18T16:00:00"
}
```

---

##  Événements RabbitMQ

### Messages Entrants (Service reçoit)

####  Demande de Validation depuis ServiceAgence
**Queue :** `Validation-Demande-Queue`
```json
{
  "eventId": "evt_12345",
  "idClient": "CLI_001",
  "idAgence": "AG_YAO_001",
  "cni": "123456789012",
  "email": "client@example.com",
  "nom": "NGAMBA",
  "prenom": "Jean",
  "numero": "695123456",
  "rectoCniHash": "hash_recto_123",
  "versoCniHash": "hash_verso_456",
  "agenceValidation": {
    "kycValid": true,
    "documentsValid": true,
    "formatValid": true,
    "qualityScore": 85,
    "validationNotes": ["Documents authentiques", "Photos de bonne qualité"],
    "validatedAt": "2024-06-18T10:25:00",
    "validatedBy": "AGENCE_KYC_SYSTEM"
  },
  "sourceService": "ServiceAgence",
  "timestamp": "2024-06-18T10:30:00"
}
```

### Messages Sortants (Service envoie)

####  Réponse de Validation vers ServiceAgence
**Exchange :** `Demande-exchange`
**Routing Key :** `demande.validation.response`

**Approbation :**
```json
{
  "eventId": "evt_12345",
  "idClient": "CLI_001",
  "idAgence": "AG_YAO_001",
  "email": "client@example.com",
  "statut": "APPROVED",
  "message": "Demande approuvée après analyse complète",
  "limiteDailyWithdrawal": "1000000.00",
  "limiteDailyTransfer": "2000000.00",
  "limiteMonthlyOperations": "10000000.00",
  "riskScore": 35,
  "riskLevel": "LOW",
  "timestamp": "2024-06-18T10:40:00",
  "targetService": "ServiceAgence"
}
```

**Rejet :**
```json
{
  "eventId": "evt_12345",
  "idClient": "CLI_001",
  "idAgence": "AG_YAO_001",
  "email": "client@example.com",
  "statut": "REJECTED",
  "probleme": "RISQUE_CRITIQUE",
  "message": "Score de risque trop élevé: 85 - REJET RECOMMANDÉ - Risque critique détecté",
  "timestamp": "2024-06-18T10:40:00",
  "targetService": "ServiceAgence"
}
```

**Révision Manuelle :**
```json
{
  "eventId": "evt_12345",
  "idClient": "CLI_001",
  "idAgence": "AG_YAO_001",
  "email": "client@example.com",
  "statut": "MANUAL_REVIEW",
  "message": "RÉVISION MANUELLE OBLIGATOIRE - Plusieurs indicateurs de risque",
  "timestamp": "2024-06-18T10:40:00",
  "targetService": "ServiceAgence"
}
```

---

## 🚨 Codes d'Erreur et Statuts

### Statuts de Demande
| Statut | Description | Action Frontend |
|--------|-------------|-----------------|
| `RECEIVED` | Demande reçue | Afficher "En cours de réception" |
| `ANALYZING` | En cours d'analyse | Afficher "Analyse en cours" |
| `MANUAL_REVIEW` | Révision manuelle requise | Afficher dans la queue superviseur |
| `APPROVED` | Approuvée | Afficher succès avec limites |
| `REJECTED` | Rejetée | Afficher raison du rejet |
| `EXPIRED` | Expirée | Proposer nouvelle demande |

### Niveaux de Risque
| Niveau | Score | Couleur | Action |
|--------|-------|---------|--------|
| `LOW` | 0-30 | 🟢 Vert | Approbation automatique |
| `MEDIUM` | 31-60 | 🟡 Jaune | Limites réduites |
| `HIGH` | 61-80 | 🟠 Orange | Révision manuelle |
| `CRITICAL` | 81-100 | 🔴 Rouge | Rejet automatique |

### Codes d'Erreur Métier
| Code | Description | Solution |
|------|-------------|----------|
| `FORMAT_CNI_INCORRECT` | CNI invalide (8-12 chiffres) | Corriger le format |
| `FORMAT_NUMERO_INCORRECT` | Numéro invalide (6XXXXXXXX) | Format camerounais requis |
| `FORMAT_EMAIL_INCORRECT` | Email invalide | Corriger l'email |
| `RISQUE_CRITIQUE` | Score > 80 | Révision complète nécessaire |
| `CNI_DEJA_UTILISEE` | CNI déjà enregistrée | Vérifier les doublons |
| `EMAIL_MULTIPLE_DEMANDES` | Trop de demandes récentes | Attendre ou investiguer |

---



### Collection Postman
```json
{
  "info": {
    "name": "ServiceDemande Supervision API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8081"
    },
    {
      "key": "supervisorAuth",
      "value": "Basic c3VwZXJ2aXNvcjpzdXBlcnZpc29yMTIz"
    }
  ],
  "item": [
    {
      "name": "Dashboard",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "{{supervisorAuth}}"
          }
        ],
        "url": "{{baseUrl}}/api/v1/demande/dashboard"
      }
    },
    {
      "name": "Pending Reviews",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "{{supervisorAuth}}"
          }
        ],
        "url": "{{baseUrl}}/api/v1/demande/manual-review/pending"
      }
    }
  ]
}
```

---

##  Monitoring et Logs

### Endpoints de Monitoring
```http
GET /actuator/health       # Santé générale
GET /actuator/info         # Informations version
GET /actuator/metrics      # Métriques
```

### Logs Importants
```bash
# Anti-fraude
[INFO] 🔍 Réception demande validation: client=CLI_001, agence=AG_YAO_001
[INFO] Analyse terminée - Score: 65, Niveau: HIGH, Flags: [EMAIL_MULTIPLE_DEMANDES]

# Révisions manuelles
[INFO] 👨‍💼 Révision manuelle: demande=demande_12345, approuvé=true, reviewer=SUPERVISOR_001

# Statistiques
[INFO] Statistiques générées: 1247 demandes total, 87.5% taux approbation
```

