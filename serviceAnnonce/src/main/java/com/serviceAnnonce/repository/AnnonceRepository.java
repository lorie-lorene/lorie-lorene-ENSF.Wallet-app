package com.serviceAnnonce.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import com.serviceAnnonce.model.Annonce;

@RepositoryRestResource
public interface AnnonceRepository extends MongoRepository<Annonce, String> {

}
