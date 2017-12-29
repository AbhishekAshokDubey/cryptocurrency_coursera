package com.assignment.consensus_from_trust;

public class Candidate {
	Transaction tx;
	int sender;
	
	public Candidate(Transaction tx, int sender) {
		this.tx = tx;
		this.sender = sender;
	}
}