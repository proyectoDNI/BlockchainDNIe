package blockchaindni.e;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
// clase block o bloque que contiene cada uno de los elementos de un bloque
public class Block {
    //hash anterior
    public String PHash;
    public int TimeStamp;
    public int Nonce;
    public String MerkleRoot;
    public List<String> Transacciones = new ArrayList<>(); 
    public int Index;
    //hash del bloque
    public String HHash;
    public List<List<String>>MerkleTree;
    //función con la que se calcula el hash del bloque
    public String calcularhash (){
        String cadena =String.join("", Transacciones);
        String Bloque;
        Bloque = TimeStamp+Integer.toString(Nonce)+MerkleRoot+cadena+PHash;
        String hash256="";
        try {
            MessageDigest md = MessageDigest.getInstance( "SHA-256" );
            md.update(Bloque.getBytes());
            md.update(md.digest());
            byte[] digest = md.digest();
            hash256 = String.format( "%064x", new BigInteger( 1, digest ) );
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(BlockchainDNIE.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return hash256;
    }
    @SuppressWarnings("empty-statement")
    //función o constructor con el cual se inicializa o se crea el bloque por primera vez
    public Block (int index, int timestamp, String transaccion, String hashA){
        byte [] aux= new byte [] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        Index=index;
        TimeStamp=timestamp;
        Nonce=0;
        MerkleRoot=String.format( "%064x", new BigInteger( 1, aux));;
        if (!transaccion.equals(""))
            Transacciones.add(transaccion);
        PHash= hashA;
        MerkleTree=null;
    }
}
