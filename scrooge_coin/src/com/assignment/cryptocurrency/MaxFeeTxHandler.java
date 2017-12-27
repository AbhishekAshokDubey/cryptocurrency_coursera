package com.assignment.cryptocurrency;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Collections;
import java.util.Set;

public class MaxFeeTxHandler {

    private UTXOPool utxoPool;

    public MaxFeeTxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    public boolean isValidTx(Transaction tx) {
        List<UTXO> utxo_used_this_tx = new ArrayList<UTXO>();
        double input_sum = 0;
        double output_sum = 0;
        // (1) + (2) + (3)
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input inp_i = tx.getInput(i);
            UTXO utxo_i = new UTXO(inp_i.prevTxHash, inp_i.outputIndex);

            // (1)
            if (!(this.utxoPool.contains(utxo_i))) {
                return false;
            }

            // (2)
            Transaction.Output previous_tx_output_corresponding_to_inp_i = utxoPool.getTxOutput(utxo_i);
            PublicKey public_key = previous_tx_output_corresponding_to_inp_i.address;
            // Raw message for current transaction
            byte[] raw_tx_message = tx.getRawDataToSign(i);
            if (!(Crypto.verifySignature(public_key, raw_tx_message, inp_i.signature))) {
                return false;
            }

            // (3)
            if (utxo_used_this_tx.contains(utxo_i)) {
                return false;
            }

            // other useful book-keeping
            utxo_used_this_tx.add(utxo_i);
            input_sum += previous_tx_output_corresponding_to_inp_i.value;
        }

        // (4) + (5)
        for (int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output out_i = tx.getOutput(i);
            if (out_i.value < 0) {
                return false;
            }
            output_sum += out_i.value;
        }
        return (input_sum >= output_sum);
    }

    public double calTxDiff(Transaction tx){
        if (tx==null) return 0;

        double input_sum = 0;
        double output_sum = 0;
        for (Transaction.Input inp_i: tx.getInputs()) {
                UTXO utxo_i = new UTXO(inp_i.prevTxHash, inp_i.outputIndex);
                Transaction.Output previous_tx_output_corresponding_to_inp_i = utxoPool.getTxOutput(utxo_i);
                if(previous_tx_output_corresponding_to_inp_i!=null){
                    input_sum += previous_tx_output_corresponding_to_inp_i.value;
            }
        }
        for (Transaction.Output out_i: tx.getOutputs()) {
                output_sum += out_i.value;
        }
        return input_sum-output_sum;
    }

    public Set<Transaction> sortmap(Transaction[] possibleTxs){

        Set<Transaction> sortedValidTxsSet = new TreeSet<>((tx1, tx2) -> {
            double tx1Fees = calTxDiff(tx1);
            double tx2Fees = calTxDiff(tx2);
            return Double.valueOf(tx2Fees).compareTo(tx1Fees);
        });

        Collections.addAll(sortedValidTxsSet, possibleTxs);
        return sortedValidTxsSet;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        double max_tx_fee_value = 0;
        double fee = 0;
        List<Transaction> maxFeeValidTxsList = new ArrayList<>();
        List<Transaction> validTxsList = new ArrayList<>();

        // Sort array: // http://www.java2novice.com/java-interview-programs/sort-a-map-by-value/
        // or
         Set<Transaction> sorted_txs = sortmap(possibleTxs);

         for (Transaction tx : sorted_txs) {
        //for (Transaction tx : possibleTxs) {
            if (!isValidTx(tx)) {
                // skip this tx
                continue;
            }
            validTxsList.add(tx);

            // update the utxopool for the used inputs
            for (Transaction.Input input : tx.getInputs()) {
                UTXO used_utxo = new UTXO(input.prevTxHash, input.outputIndex);
                this.utxoPool.removeUTXO(used_utxo);
            }
            byte[] txHash = tx.getHash();
            for (int i = 0; i < tx.numOutputs(); i++) {
                Transaction.Output out_i = tx.getOutput(i);
                UTXO new_utxo = new UTXO(txHash, i);
                this.utxoPool.addUTXO(new_utxo, out_i);
            }

            fee = calTxDiff(tx);

            if (fee == max_tx_fee_value){
                maxFeeValidTxsList.add(tx);
            }
            else if(fee > max_tx_fee_value){
                max_tx_fee_value = fee;
                maxFeeValidTxsList.clear();
                maxFeeValidTxsList.add(tx);
            }
            //
        }
        return validTxsList.toArray(new Transaction[validTxsList.size()]);
        //return maxFeeValidTxsList.toArray(new Transaction[maxFeeValidTxsList.size()]);
    }

}