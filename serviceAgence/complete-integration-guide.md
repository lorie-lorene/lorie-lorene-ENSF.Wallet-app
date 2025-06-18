#  Guide Complet d'Intégration - Service Agence Bancaire

## Vue d'Ensemble

Le **Service Agence** est un microservice bancaire complet qui gère les agences, comptes clients, transactions et validation KYC. Il expose une API REST sécurisée et communique via RabbitMQ pour les événements asynchrones.

###  Architecture
- **Framework** : Spring Boot 3.x
- **Base de données** : MongoDB
- **Messaging** : RabbitMQ
- **Sécurité** : Spring Security avec JWT
- **Documentation** : Swagger/OpenAPI 3.0

---

##  Configuration Base

### URL de Base
```
Développement : http://localhost:8080
Production : https://api-agence.production.com
```

### Headers Requis
```http
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
Accept: application/json
```

---

##  Authentification & Sécurité

###  IMPORTANT - Identifiants Générés Automatiquement

Le service utilise **Spring Security avec génération automatique** des identifiants au démarrage.

**Configuration d'authentification :**
- **Username** : `user` ou `AGENCE` (selon la configuration)
- **Password** :  **Généré automatiquement à chaque démarrage**
- **Durée du token** : 24 heures
- **Type** : JWT Bearer Token

** Comment récupérer le mot de passe :**

1. **Via les logs de l'application :**
```bash
# Docker
docker logs service-agence 2>&1 | grep -i "password"

# Application locale
tail -f logs/application.log | grep -i "password"

# Systemd
journalctl -u service-agence | grep -i "password"
```

2. **Rechercher dans les logs de démarrage :**
```
2024-06-18 09:00:15.123 INFO --- Using generated security password: a8b9c3d4e5f6
2024-06-18 09:00:15.124 INFO --- Default user 'user' password: a8b9c3d4e5f6
```

3. **Via l'endpoint actuator (si activé) :**
```http
GET /actuator/env/spring.security.user.password
```

### Rôles Disponibles
- **ADMIN** : Accès complet à toutes les fonctionnalités
- **AGENCE** : Gestion des comptes et transactions de l'agence
- **CLIENT** : Accès limité aux informations personnelles

### Obtention du Token JWT

 **IMPORTANT** : Les identifiants sont générés automatiquement au démarrage de l'application.

**Identifiants par défaut :**
- **Username** : `user` ou `AGENCE`
- **Password** :  **Généré automatiquement et affiché dans les logs au démarrage**

**Récupération du mot de passe :**
```bash
# Vérifiez les logs de démarrage de l'application
docker logs service-agence | grep "password"
# ou
tail -f application.log | grep "Generated password"
```

**Exemple de log au démarrage :**
```
2024-06-18 09:00:15.123 INFO --- Using generated security password: a8b9c3d4e5f6
2024-06-18 09:00:15.124 INFO --- Default user created: username=user, password=a8b9c3d4e5f6
```

**Connexion avec les identifiants générés :**
```http
POST /auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "a8b9c3d4e5f6"
}
```

**Réponse :**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 86400,
  "roles": ["AGENCE"]
}
```

---

##  API Endpoints Complets

###  **1. Gestion des Agences**

####  Statistiques d'une Agence
```http
GET /api/v1/agence/{idAgence}/statistics
Authorization: Bearer {token}
Roles: AGENCE, ADMIN
```

**Exemple de réponse :**
```json
{
  "idAgence": "AG001",
  "nomAgence": "Agence Yaoundé Centre",
  "totalComptes": 1247,
  "comptesActifs": 1189,
  "comptesSuspendus": 45,
  "comptesBloqués": 13,
  "totalSoldes": "850000000.00",
  "totalTransactions": 15678,
  "totalVolume": "12500000000.00",
  "capital": "500000000.00",
  "soldeDisponible": "485000000.00",
  "generatedAt": "2024-06-18T10:30:00"
}
```

####  Informations d'une Agence
```http
GET /api/v1/agence/{idAgence}/info
```

**Réponse :**
```json
{
  "idAgence": "AG001",
  "codeAgence": "YAO001",
  "nom": "Agence Yaoundé Centre",
  "adresse": "Avenue Kennedy, Yaoundé",
  "ville": "Yaoundé",
  "email": "yaounde.centre@banque.cm",
  "telephone": "+237699123456",
  "status": "ACTIVE",
  "capital": "500000000.00",
  "soldeDisponible": "485000000.00",
  "createdAt": "2023-01-15T09:00:00"
}
```

####  Validation des Limites
```http
POST /api/v1/agence/{idAgence}/validate-limits?montant=5000000
Authorization: Bearer {token}
Roles: AGENCE
```

###  **2. Gestion des Comptes**

#### Liste des Comptes d'une Agence
```http
GET /api/v1/agence/{idAgence}/comptes?limit=50
Authorization: Bearer {token}
Roles: AGENCE, ADMIN
```

**Réponse :**
```json
[
  {
    "id": "comp_001",
    "numeroCompte": 237001240618001,
    "idClient": "CLI_12345",
    "idAgence": "AG001",
    "solde": "850000.00",
    "status": "ACTIVE",
    "type": "STANDARD",
    "limiteDailyWithdrawal": "1000000.00",
    "limiteDailyTransfer": "2000000.00",
    "createdAt": "2024-06-18T08:30:00",
    "lastTransactionAt": "2024-06-18T14:25:00",
    "totalTransactions": 45,
    "totalVolume": "15500000.00"
  }
]
```

####  Recherche de Compte par Numéro
```http
GET /api/v1/agence/comptes/{numeroCompte}
Authorization: Bearer {token}
Roles: AGENCE, ADMIN
```

####  Consultation du Solde
```http
GET /api/v1/agence/comptes/{numeroCompte}/solde
Authorization: Bearer {token}
Roles: AGENCE, ADMIN
```

**Réponse :**
```json
{
  "numeroCompte": "237001240618001",
  "solde": "850000.00",
  "devise": "FCFA",
  "timestamp": "2024-06-18T15:30:00"
}
```

####  Historique des Transactions
```http
GET /api/v1/agence/comptes/{numeroCompte}/transactions?limit=20
Authorization: Bearer {token}
Roles: AGENCE, ADMIN
```

####  Activation d'un Compte
```http
PUT /api/v1/agence/comptes/{numeroCompte}/activate?activatedBy=USER_ID
Authorization: Bearer {token}
Roles: AGENCE
```

#### ⏸️Suspension d'un Compte
```http
PUT /api/v1/agence/comptes/{numeroCompte}/suspend
Authorization: Bearer {token}
Roles: AGENCE

{
  "reason": "Activité suspecte détectée",
  "suspendedBy": "AGENT_001"
}
```

####  Recherche Avancée
```http
GET /api/v1/agence/{idAgence}/comptes/search?status=ACTIVE&clientId=CLI_123&page=0&size=20
Authorization: Bearer {token}
Roles: AGENCE, ADMIN
```

###  **3. Gestion des Transactions**

####  Exécuter une Transaction
```http
POST /api/v1/agence/transactions
Authorization: Bearer {token}
Roles: AGENCE
Content-Type: application/json

{
  "type": "RETRAIT_PHYSIQUE",
  "montant": "100000.00",
  "compteSource": "237001240618001",
  "compteDestination": null,
  "idClient": "CLI_12345",
  "idAgence": "AG001",
  "description": "Retrait espèces guichet",
  "referenceExterne": "REF_20240618_001"
}
```

**Types de Transaction Disponibles :**
- `DEPOT_PHYSIQUE` - Dépôt en espèces (gratuit)
- `RETRAIT_PHYSIQUE` - Retrait en espèces (frais 1.5%)
- `RETRAIT_MOBILE_MONEY` - Retrait Mobile Money (frais 2.5%)
- `TRANSFERT_INTERNE` - Transfert entre comptes internes (frais 1%)
- `TRANSFERT_EXTERNE` - Transfert vers banque externe (frais 3%)

**Réponse Succès :**
```json
{
  "success": true,
  "transactionId": "TXN_1718712345_A1B2C3D4",
  "montant": "100000.00",
  "frais": "1500.00",
  "message": "Transaction réussie",
  "timestamp": "2024-06-18T15:45:30"
}
```

**Réponse Erreur :**
```json
{
  "success": false,
  "errorCode": "SOLDE_INSUFFISANT",
  "message": "Solde insuffisant. Requis: 101500 FCFA, Disponible: 50000 FCFA",
  "timestamp": "2024-06-18T15:45:30"
}
```

####  Estimation des Frais
```http
POST /api/v1/agence/transactions/estimate-frais
Authorization: Bearer {token}
Roles: AGENCE, CLIENT

{
  "type": "RETRAIT_PHYSIQUE",
  "montant": "100000.00",
  "idAgence": "AG001"
}
```

**Réponse :**
```json
{
  "montantBrut": "100000.00",
  "frais": "1500.00",
  "montantNet": "98500.00",
  "total": "101500.00"
}
```

###  **4. Validation KYC**

####  Validation Manuelle des Documents
```http
POST /api/v1/agence/kyc/validate
Authorization: Bearer {token}
Roles: AGENCE
Content-Type: multipart/form-data

idClient=CLI_12345
cni=123456789012
rectoCni={binary_data}
versoCni={binary_data}
```

**Réponse :**
```json
{
  "valid": true,
  "errorCode": "DOCUMENTS_CONFORMES",
  "reason": "Documents validés avec succès",
  "scoreQualite": 85,
  "documentsValidated": ["CNI_RECTO", "CNI_VERSO"]
}
```

####  Rapport KYC
```http
GET /api/v1/agence/kyc/{idClient}/report
Authorization: Bearer {token}
Roles: AGENCE, ADMIN
```

###  **5. Configuration et Système**

####  Santé du Service
```http
GET /api/v1/agence/health
```

**Réponse :**
```json
{
  "status": "UP",
  "service": "AgenceService",
  "version": "2.0.0",
  "timestamp": "2024-06-18T16:00:00",
  "dependencies": {
    "mongodb": "UP",
    "rabbitmq": "UP"
  }
}
```

####  Configuration des Frais
```http
GET /api/v1/agence/config/frais
```

**Réponse :**
```json
{
  "fraisDepotPhysique": "0%",
  "fraisRetraitPhysique": "1.5% (min 100 FCFA)",
  "fraisRetraitMobileMoney": "2.5% (min 150 FCFA)",
  "fraisTransfertInterne": "1% (min 50 FCFA)",
  "fraisTransfertExterne": "3% (min 500 FCFA)",
  "tva": "17.5%",
  "fraisTenueCompte": "500 FCFA/mois"
}
```

---

##  Événements RabbitMQ

### Messages Entrants (Service reçoit)

####  Demande de Création de Compte
**Queue :** `user-registration-queue`
```json
{
  "eventId": "evt_12345",
  "idClient": "CLI_12345",
  "idAgence": "AG001",
  "cni": "123456789012",
  "email": "client@example.com",
  "nom": "NGAMBA",
  "prenom": "Jean",
  "numero": "699123456",
  "rectoCni": "base64_encoded_image",
  "versoCni": "base64_encoded_image",
  "sourceService": "UserService",
  "timestamp": "2024-06-18T10:00:00"
}
```

####  Demande de Transaction
**Queue :** `user-transaction-queue`
```json
{
  "eventId": "evt_67890",
  "type": "RETRAIT_PHYSIQUE",
  "montant": "100000.00",
  "numeroClient": "CLI_12345",
  "numeroCompte": "237001240618001",
  "numeroCompteDestination": null,
  "sourceService": "UserService",
  "timestamp": "2024-06-18T14:30:00"
}
```

### Messages Sortants (Service envoie)

#### Réponse Création de Compte
**Exchange :** `agence-exchange`
**Routing Key :** `agence.registration.response`
```json
{
  "eventId": "resp_12345",
  "idClient": "CLI_12345",
  "idAgence": "AG001",
  "email": "client@example.com",
  "statut": "ACCEPTE",
  "numeroCompte": 237001240618001,
  "timestamp": "2024-06-18T10:15:00",
  "targetService": "UserService"
}
```

####  Réponse Transaction
**Exchange :** `agence-exchange`
**Routing Key :** `agence.transaction.response`
```json
{
  "eventId": "resp_67890",
  "transactionId": "TXN_1718712345_A1B2C3D4",
  "statut": "SUCCESS",
  "message": "Transaction réussie",
  "montant": "100000.00",
  "frais": "1500.00",
  "numeroCompte": "237001240618001",
  "timestamp": "2024-06-18T14:35:00",
  "targetService": "UserService"
}
```

---

##  Codes d'Erreur

### Erreurs de Transaction
| Code 		       | Description                           | Solution 
|-----------------------------|---------------------------------------|----------
| `SOLDE_INSUFFISANT`         | Solde insuffisant pour la transaction | Vérifier le solde avant transaction 
| `COMPTE_INACTIF`            | Compte non actif                      | Activer le compte 
| `LIMITE_RETRAIT_DEPASSEE`   | Limite quotidienne dépassée           | Attendre le lendemain 
| `COMPTE_DESTINATION_REQUIS` | Compte destination manquant           | Fournir le compte destination 
| `MONTANT_INVALIDE`          | Montant négatif ou zéro               | Utiliser un montant positif 
| `MONTANT_TROP_ELEVE`        | Montant > 50M FCFA                    | Réduire le montant 

### Erreurs de Compte
| Code 		    | Description 		       | Solution 
|--------------------------|---------------------------------|----------
| `COMPTE_INTROUVABLE`     | Numéro de compte inexistant     | Vérifier le numéro 
| `COMPTE_DEJA_EXISTANT`   | Compte déjà créé pour ce client | Utiliser le compte existant 
| `NUMERO_COMPTE_INVALIDE` | Format invalide                 | Utiliser un numéro valide 

### Erreurs KYC
| Code 		         | Description 	      | Solution 
|-------------------------------|---------------------------|----------
| `FORMAT_CNI_INCORRECT` 	 | Format CNI invalide       | Corriger le format 
| `CNI_DEJA_UTILISEE`           | CNI déjà associée         | Vérifier l'unicité 
| `QUALITE_IMAGE_INSUFFISANTE`  | Image de mauvaise qualité | Améliorer la qualité 
| `FRAUDE_DETECTEE`             | Document suspect          | Vérification manuelle 

---

##  Configuration Frontend

### Variables d'Environnement
```javascript
// .env
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_API_VERSION=v1
REACT_APP_RABBITMQ_WS_URL=ws://localhost:15674/ws
```

### Configuration Frontend avec Authentification Auto

### Service API (JavaScript/TypeScript)
```javascript
class AgenceApiService {
  constructor(baseURL) {
    this.baseURL = baseURL;
    this.token = null;
  }

  // Authentification avec identifiants générés
  async authenticate(username = 'user', password) {
    if (!password) {
      throw new Error('Password requis - Vérifiez les logs de démarrage');
    }

    const response = await fetch(`${this.baseURL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ username, password })
    });

    if (!response.ok) {
      throw new Error('Échec authentification - Vérifiez username/password');
    }

    const authData = await response.json();
    this.token = authData.token;
    
    // Stocker le token pour les requêtes suivantes
    localStorage.setItem('agence_token', authData.token);
    localStorage.setItem('agence_token_expires', 
      Date.now() + (authData.expiresIn * 1000));
    
    return authData;
  }

  // Vérifier si le token est valide
  isAuthenticated() {
    const token = localStorage.getItem('agence_token');
    const expires = localStorage.getItem('agence_token_expires');
    
    if (!token || !expires) return false;
    if (Date.now() > parseInt(expires)) return false;
    
    this.token = token;
    return true;
  }

  async makeRequest(endpoint, options = {}) {
    // Auto-authentification si token disponible
    if (!this.token && localStorage.getItem('agence_token')) {
      if (!this.isAuthenticated()) {
        throw new Error('Token expiré - Reconnexion requise');
      }
    }

    if (!this.token) {
      throw new Error('Authentification requise');
    }

    const url = `${this.baseURL}/api/v1/agence${endpoint}`;
    const config = {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.token}`,
        ...options.headers
      },
      ...options
    };

    const response = await fetch(url, config);
    
    if (response.status === 401) {
      // Token expiré
      localStorage.removeItem('agence_token');
      localStorage.removeItem('agence_token_expires');
      throw new Error('Session expirée - Reconnexion requise');
    }
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Erreur API');
    }
    
    return response.json();
  }

  // Méthodes d'API (inchangées)
  async getAgenceAccounts(idAgence, limit = 50) {
    return this.makeRequest(`/${idAgence}/comptes?limit=${limit}`);
  }

  async executeTransaction(transactionData) {
    return this.makeRequest('/transactions', {
      method: 'POST',
      body: JSON.stringify(transactionData)
    });
  }
  
  // ... autres méthodes
}
```

### Hook React pour Authentification
```jsx
import { useState, useEffect, createContext, useContext } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [auth, setAuth] = useState(null);
  const [loading, setLoading] = useState(true);
  
  const apiService = new AgenceApiService(process.env.REACT_APP_API_BASE_URL);

  useEffect(() => {
    // Vérifier token au démarrage
    if (apiService.isAuthenticated()) {
      setAuth({ authenticated: true, service: apiService });
    }
    setLoading(false);
  }, []);

  const login = async (password) => {
    try {
      const authData = await apiService.authenticate('user', password);
      setAuth({ authenticated: true, service: apiService, ...authData });
      return authData;
    } catch (error) {
      throw error;
    }
  };

  const logout = () => {
    localStorage.removeItem('agence_token');
    localStorage.removeItem('agence_token_expires');
    setAuth(null);
  };

  return (
    <AuthContext.Provider value={{ auth, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
```

### Composant de Connexion
```jsx
import React, { useState } from 'react';
import { useAuth } from './AuthProvider';

const LoginForm = () => {
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      await login(password);
      // Redirection automatique gérée par le provider
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-info">
        <h3>ℹ️ Informations de Connexion</h3>
        <p><strong>Username:</strong> user</p>
        <p><strong>Password:</strong> Vérifiez les logs du service</p>
        <code>docker logs service-agence | grep password</code>
      </div>

      <form onSubmit={handleSubmit} className="login-form">
        <h2>Connexion Service Agence</h2>
        
        <div className="form-group">
          <label>Username</label>
          <input 
            type="text" 
            value="user" 
            disabled 
            className="form-control"
          />
        </div>

        <div className="form-group">
          <label>Password (des logs)</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Coller le password des logs"
            className="form-control"
            required
          />
        </div>

        {error && (
          <div className="alert alert-danger">
            ❌ {error}
          </div>
        )}

        <button 
          type="submit" 
          disabled={loading || !password}
          className="btn btn-primary"
        >
          {loading ? 'Connexion...' : 'Se Connecter'}
        </button>
      </form>
    </div>
  );
};
```

### Composant React Exemple
```jsx
import React, { useState, useEffect } from 'react';

const TransactionForm = ({ agenceId, onSuccess }) => {
  const [formData, setFormData] = useState({
    type: 'RETRAIT_PHYSIQUE',
    montant: '',
    compteSource: '',
    description: ''
  });
  const [estimation, setEstimation] = useState(null);
  const [loading, setLoading] = useState(false);

  const apiService = new AgenceApiService(
    process.env.REACT_APP_API_BASE_URL,
    localStorage.getItem('token')
  );

  const handleEstimate = async () => {
    try {
      const estimate = await apiService.estimateFees({
        ...formData,
        idAgence: agenceId
      });
      setEstimation(estimate);
    } catch (error) {
      console.error('Erreur estimation:', error);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const result = await apiService.executeTransaction({
        ...formData,
        idAgence: agenceId
      });
      
      onSuccess(result);
    } catch (error) {
      console.error('Erreur transaction:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <select 
        value={formData.type}
        onChange={(e) => setFormData({...formData, type: e.target.value})}
      >
        <option value="DEPOT_PHYSIQUE">Dépôt Physique</option>
        <option value="RETRAIT_PHYSIQUE">Retrait Physique</option>
        <option value="TRANSFERT_INTERNE">Transfert Interne</option>
      </select>

      <input
        type="number"
        placeholder="Montant (FCFA)"
        value={formData.montant}
        onChange={(e) => setFormData({...formData, montant: e.target.value})}
        onBlur={handleEstimate}
      />

      <input
        type="text"
        placeholder="Numéro de compte"
        value={formData.compteSource}
        onChange={(e) => setFormData({...formData, compteSource: e.target.value})}
      />

      {estimation && (
        <div className="estimation">
          <p>Frais: {estimation.frais} FCFA</p>
          <p>Total à débiter: {estimation.total} FCFA</p>
        </div>
      )}

      <button type="submit" disabled={loading}>
        {loading ? 'Traitement...' : 'Exécuter Transaction'}
      </button>
    </form>
  );
};
```

---

## 🧪 Tests et Validation

### Tests d'Endpoints avec curl

#### Test de Santé
```bash
curl -X GET http://localhost:8080/api/v1/agence/health
```

#### Test d'Authentification
```bash
# ⚠️ Remplacez PASSWORD_GENERE par le mot de passe affiché dans les logs
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"PASSWORD_GENERE"}'

# Exemple avec mot de passe généré
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"a8b9c3d4e5f6"}'
```

#### Test de Transaction
```bash
curl -X POST http://localhost:8080/api/v1/agence/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "type": "RETRAIT_PHYSIQUE",
    "montant": "50000.00",
    "compteSource": "237001240618001",
    "idClient": "CLI_12345",
    "idAgence": "AG001",
    "description": "Test retrait"
  }'
```

### Collection Postman
```json
{
  "info": {
    "name": "Service Agence API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    },
    {
      "key": "token",
      "value": ""
    },
    {
      "key": "generatedPassword",
      "value": "REMPLACER_PAR_PASSWORD_DES_LOGS"
    }
  ],
  "item": [
    {
      "name": "Auth Login",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"username\": \"user\",\n  \"password\": \"{{generatedPassword}}\"\n}"
        },
        "url": "{{baseUrl}}/auth/login"
      }
    },
    {
      "name": "Get Account Balance",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "url": "{{baseUrl}}/api/v1/agence/comptes/237001240618001/solde"
      }
    }
  ]
}
```

---

## 📊 Monitoring et Logs

### Endpoints de Monitoring
```http
GET /actuator/health       # Santé générale
GET /actuator/metrics      # Métriques
GET /actuator/info         # Informations version
GET /actuator/prometheus   # Métriques Prometheus
```

### Logs Importants
```bash
# Transactions
[INFO] Transaction réussie [TXN_xxx]: 100000 FCFA

# Erreurs métier
[WARN] SOLDE INSUFFISANT: Requis: 101500, Disponible: 50000

# Sécurité
[ERROR] Tentative d'accès non autorisé: user=xxx, endpoint=xxx

# Performance
[DEBUG] Traitement transaction: 245ms
```

---

## 🚀 Déploiement et Configuration

### Configuration application.yml
```yaml
server:
  port: 8080

spring:
  application:
    name: service-agence
  data:
    mongodb:
      host: localhost
      port: 27017
      database: agence_bancaire
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

logging:
  level:
    com.serviceAgence: DEBUG
    root: INFO

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

### Variables d'Environnement Production
```bash
export MONGODB_URI=mongodb://prod-mongo:27017/agence_bancaire
export RABBITMQ_URL=amqp://user:pass@prod-rabbit:5672
export JWT_SECRET=your-secret-key
export API_BASE_URL=https://api-agence.production.com
```

---

## 📞 Support et Contact

### Documentation Swagger
Une fois le service démarré, accédez à :
- **URL** : http://localhost:8080/swagger-ui.html
- **OpenAPI JSON** : http://localhost:8080/v3/api-docs

### Support Technique
- **Email** : dev@agence.com
- **Documentation** : https://docs.agence.com
- **Status Page** : https://status.agence.com

### Versions Supportées
- **Version Actuelle** : 2.0.0
- **Compatibilité** : API v1.x rétro-compatible
- **Maintenance** : Support LTS jusqu'en 2026

---

*Ce guide couvre l'intégration complète du Service Agence. Pour des questions spécifiques, consultez la documentation Swagger ou contactez l'équipe de développement.*
