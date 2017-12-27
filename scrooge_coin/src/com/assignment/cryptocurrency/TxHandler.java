package com.assignment.cryptocurrency;

import java.security.PublicKey;
import java.util.List;
import java.util.ArrayList;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed (consumed from previous transactions) by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     * @code tx is the current transaction
     */
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


    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        List<Transaction> validTxsList = new ArrayList<>();
        for (Transaction tx : possibleTxs) {
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

        }
        return validTxsList.toArray(new Transaction[validTxsList.size()]);
    }

}
