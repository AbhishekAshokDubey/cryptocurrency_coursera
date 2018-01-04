package com.assignment.block_chain;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {

    // Blocks which are added into the main block chain
    // Now to refer to these block anytime later we need its
    // 1. index (to avoid traversing through entire chain)
    // 2. utxoPool at that block's time (to validate the transactions referring to this block)
    private class BlockPlus{
        public Block block;
        public int index;
        private UTXOPool utxoPool;
        BlockPlus(Block block, int index, UTXOPool utxoPool){
            this.block = block;
            this.index = index;
            this.utxoPool = utxoPool;
        }
        // making sure that utxoPool does not get changed from anywhere.
        // We generate utxopool for the children Blocks from the parent blocks utxopool
        public UTXOPool getUTXOPoolCopy(){
            return new UTXOPool(this.utxoPool);
        }
    }

    public static final int CUT_OFF_AGE = 10;
    private HashMap<ByteArrayWrapper, BlockPlus> blockChain = new HashMap<>();
    private TransactionPool txPool = new TransactionPool();
    private BlockPlus lastBlockPlus;


    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        UTXOPool utxoPool = new UTXOPool();
        BlockPlus BlockPlusToAdd = new BlockPlus(genesisBlock,1, utxoPool);
        ByteArrayWrapper hashContentByteWrapper = new ByteArrayWrapper(genesisBlock.getHash());
        blockChain.put(hashContentByteWrapper, BlockPlusToAdd);
        lastBlockPlus = BlockPlusToAdd;
        // Not sure the first guy gets coinbase
        addCoinbaseToUTXOPool(genesisBlock, utxoPool);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return this.lastBlockPlus.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return this.lastBlockPlus.getUTXOPoolCopy();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return this.txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // make sure the block points to valid parent Block in two parts below

        // 1. check for previous/ parent block hash
        byte[] previousBlockHash = block.getPrevBlockHash();
        if((previousBlockHash == null)){
            return false;
        }
        // 2. check for previous/ parent block
        BlockPlus parentBlockPlus = blockChain.get(new ByteArrayWrapper(previousBlockHash));
        if(parentBlockPlus==null){
            return  false;
        }

        // validate transactions in current block wrt to its parent block's utxoPool (unspent at parents time can only be spent this time)
        TxHandler txHandler = new TxHandler(parentBlockPlus.getUTXOPoolCopy());
        Transaction[] currentTxs = block.getTransactions().toArray(new Transaction[0]);

        // check all the currentTxs are valid
        // Note: handleTxs() also updates the corresponding utxoPool
        Transaction[] validTxs = txHandler.handleTxs(currentTxs);
        if(validTxs.length != currentTxs.length){
            return false;
        }

        // Looks all good, we can add the block now if it has not reached the cut_off
        if(parentBlockPlus.index + 1 <= lastBlockPlus.index - CUT_OFF_AGE){
            return false;
        }

        UTXOPool utxoPool = txHandler.getUTXOPool();
        // update utxoPool for coinbase
        addCoinbaseToUTXOPool(block, utxoPool);
        BlockPlus blockPlusToAdd = new BlockPlus(block, parentBlockPlus.index + 1, utxoPool);
        blockChain.put(new ByteArrayWrapper(block.getHash()), blockPlusToAdd);
        if(parentBlockPlus.index + 1 > lastBlockPlus.index){
            lastBlockPlus = blockPlusToAdd;
        }
        return true;
    }

    public void addCoinbaseToUTXOPool(Block block, UTXOPool utxoPool){
        // coin generated for adding a block
        Transaction tx = block.getCoinbase();
        for(int i=0; i< tx.numOutputs(); i++){
            Transaction.Output out = tx.getOutput(i);
            UTXO utxo = new UTXO(tx.getHash(), i);
            utxoPool.addUTXO(utxo, out);
        }
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }
}