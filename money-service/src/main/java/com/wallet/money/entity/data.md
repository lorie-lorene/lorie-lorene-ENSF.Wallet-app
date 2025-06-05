{
  "payer": "237657848837",
  "amount": 100,
  "externalId": "123",
  "description": "Achat d'un produit X",
  "callback": "https://7d23-129-0-44-243.ngrok-free.app"
}


{
  "reference": "73d93013-5015-46a7-8c47-c25e95c90dea",
  "status": "SUCCES",
  "message": "order-1234"
}



Test 1 : Dépôt complet
bash# 1. Initier dépôt
POST /api/deposit
{
  "payer": "237654123456",
  "amount": 1000,
  "externalId": "123"
  "description": "Test dépôt",
  "callback": "https://votre-webhook.com/webhook/freemopay"
}

# Réponse attendue :
{
  "reference": "73d93013-5015-46a7-8c47-c25e95c90dea",
  "status": "PENDING",
  "message": "SMS envoyé"
}

# 2. Vérifier statut local
GET /api/deposit/status/DEP_client123_1635789456789

# 3. Simuler webhook (client valide)
POST /webhook/freemopay
{
  "reference": "73d93013-5015-46a7-8c47-c25e95c90dea",
  "status": "SUCCESS",
  "message": "order-1234"
}

# 4. Re-vérifier statut local
GET /api/deposit/status/DEP_client123_1635789456789

# Status devrait être "SUCCESS"
Test 2 : Retrait complet
bashPOST /api/withdrawals
{
  "receiver": "237654123456",
  "amount": 5000,
  "description": "Test retrait",
  "callback": "https://votre-webhook.com/webhook/freemopay"
}