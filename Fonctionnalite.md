## service Carte( chaque requette a besoin generalement du idCarte

- Créer une nouvelle carte bancaire:

	@PostMapping("/create")
	@PreAuthorize("hasRole('CLIENT')")

	auth+ data {
	String idClient
	String idAgence
	String numeroCompte
	CarteType type
	String nomPorteur
	int codePin
	BigDecimal limiteDailyPurchase;
	BigDecimal limiteDailyWithdrawal;
	BigDecimal limiteMonthly;
	}



- lister les cartes d'un user
	@GetMapping("/my-cards")
	@PreAuthorize("hasRole('CLIENT')")

	auth



- Détails d'une carte spécifique

	@GetMapping("/{idCarte}")
	@PreAuthorize("hasRole('CLIENT')")

	auth+ data{
	idCarte
	}



-  Transfert d'argent du compte vers une carte 

	@PostMapping("/transfer-to-card")
	@PreAuthorize("hasRole('CLIENT')")

	auth+data{
	String numeroCompteSource;
	String idCarteDestination;
	BigDecimal montant;
	String description;
	}

- Bloquer une carte

	@PutMapping("/{idCarte}/block")
	@PreAuthorize("hasRole('CLIENT')")

	auth+ data{
	String idCarte
	String reason

	}
    
    
- Débloquer une carte 

	@PutMapping("/{idCarte}/unblock")
	@PreAuthorize("hasRole('CLIENT')")

	auth+ data{
	idCarte
	}

- Modifier les paramètres d'une carte

	@PutMapping("/{idCarte}/settings")
	@PreAuthorize("hasRole('CLIENT')")

	auth+ data{
	idCarte
	BigDecimal limiteDailyPurchase;
	BigDecimal limiteDailyWithdrawal;
	BigDecimal limiteMonthly;
	Boolean contactless;
	Boolean internationalPayments;
	Boolean onlinePayments;
	}

- Changer le code PIN d'une carte 
    
	@PutMapping("/{idCarte}/change-pin")
	@PreAuthorize("hasRole('CLIENT')")

	auth+data{
	idCarte
	int currentPin(4 chirffres)
	private int newPin
	}

    
- Statistiques des cartes d'un client

	@GetMapping("/statistics")
	@PreAuthorize("hasRole('CLIENT')")

	auth


- Historique des transactions d'une carte

	@GetMapping("/{idCarte}/transactions")
	@PreAuthorize("hasRole('CLIENT')")

	auth+data{
	idCarte
	}

-  Configuration des frais par type de carte (Info publique)

	@GetMapping("/config/fees")
	   
   
- Lister toutes les cartes (Admin)

	@GetMapping("/admin/all")
	@PreAuthorize("hasRole('ADMIN')")

- Bloquer/débloquer une carte (Admin)


	@PutMapping("/admin/{idCarte}/admin-block")
	@PreAuthorize("hasRole('ADMIN')")

	auth + data{
	String idCarte
	String reason

	}
    
    
- recharge d'une carte de credit par l'api money service 

	@PostMapping("/{idCarte}/recharge-orange-money")
	@PreAuthorize("hasRole('CLIENT')")

	auth+ data{
	String idCarte
	String numeroOrangeMoney
	BigDecimal montant
	private String description

	}
    
    
- Retrait depuis carte vers Orange/MTN Money
	@PostMapping("/{idCarte}/withdraw-to-mobile-money")
	@PreAuthorize("hasRole('CLIENT')")

	auth+data{
	String idCarte
	String numeroTelephone
	BigDecimal montant
	String provider // ORANGE ou MTN
	String description
	Integer codePin

	}
     
- Historique des retraits d'une carte
     
	@GetMapping("/{idCarte}/withdrawal-history")
	@PreAuthorize("hasRole('CLIENT')")


	auth+data{
	String idCarte
	} 
     
     
- Endpoint de santé du service carte
	@GetMapping("/health")

     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     

            
    
    
    
    
    
    
    
    
    
    
    
    
    
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
    
    
    
    
    
    
    
    
    
    
    
    
    
    
