package com.assignment.consensus_from_trust;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    private double p_graph;
    private double p_malicious;
    private double p_tXDistribution;
    private int numRounds;

    private boolean[] followees;

    private Set<Transaction> pendingTransactions;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_tXDistribution = p_txDistribution;
        this.numRounds = numRounds;
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        Set<Transaction> pending_Tx_toSend = new HashSet<>(this.pendingTransactions);
        this.pendingTransactions.clear();
        return pending_Tx_toSend;
    }

    // Not all Followees might be Compliant node, keep a track for good ones.
    // You can also block receiving from the senders (Nodes) which dun send you candidates in each round
    // they might be Malicious Nodes.
    // The more tricks you put, the better you score.
    public void receiveFromFollowees(Set<Candidate> candidates) {
        // IMPLEMENT THIS
        for(Candidate candidate: candidates){
            if(this.followees[candidate.sender]){
                pendingTransactions.add(candidate.tx);
            }
        }
    }
}
