/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blockchaindni.e;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Proyecto DNI
 */

//clase que permite crear el árbol de Merkle y sacar la raíz
public class MerkleTrees {
    // transaction List
  List<String> txList;
  // árbol
  List<List<String>>MerkleTree=new ArrayList<List<String>>();;
  // Merkle Root
  String root;
  
  /**
   * constructor
   * @param txList transaction List
   */
  //constructor del árbol de merkle y la clase
  public MerkleTrees(List<String> txList) {
    this.txList = txList;
    root = "";
  }
   
  /**
   * execute merkle_tree and set root.
   */
  
  // función que genera el árbol de merkle 
  public void merkle_tree() {
    
    List<String> tempTxList = new ArrayList<String>();
    
    for (int i = 0; i < this.txList.size(); i++) {
        tempTxList.add(getSHA2HexValue(this.txList.get(i)));
    }
    if (tempTxList.size()>1 && tempTxList.size()%2!=0){
        tempTxList.add(tempTxList.get(tempTxList.size()-1));
    }
    MerkleTree.add(tempTxList);
    List<String> newTxList = getNewTxList(tempTxList);
    if ( newTxList.size()>1 && newTxList.size()%2!=0){
        newTxList.add(newTxList.get(newTxList.size()-1));
    }
    MerkleTree.add(newTxList);
    while (newTxList.size() != 1) {
      newTxList = getNewTxList(newTxList);
      if ( newTxList.size()>1 && newTxList.size()%2!=0){
            newTxList.add(newTxList.get(newTxList.size()-1));
        }
      MerkleTree.add(newTxList);
    }
    
    this.root = newTxList.get(0);
  }
  
  /**
   * return Node Hash List.
   * @param tempTxList
   * @return
   */
  // función con la que se van crenado el siguiente nivele del árbol de merkle retorna una lista de texto con el siguiente nivel de hash
  private List<String> getNewTxList(List<String> tempTxList) {
    
    List<String> newTxList = new ArrayList<String>();
    int index = 0;
    while (index < tempTxList.size()) {
      // left
      String left = tempTxList.get(index);
      index++;

      // right
      String right = "";
      if (index != tempTxList.size()) {
        right = tempTxList.get(index);
      }

      // sha2 hex value
      String sha2HexValue = getSHA2HexValue(left + right);
      newTxList.add(sha2HexValue);
      index++;
      
    }
    
    return newTxList;
  }
  
  /**
   * Return hex string
   * @param str
   * @return
   */
  //función para calcular el hash 256 con él que se crea el árbol de merkle retorna un string con el valor del hash
  public String getSHA2HexValue(String str) {
        byte[] cipher_byte;
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(str.getBytes());
            md.update(md.digest());
            cipher_byte = md.digest();
            StringBuilder sb = new StringBuilder(2 * cipher_byte.length);
            for(byte b: cipher_byte) {
              sb.append(String.format("%02x", b&0xff) );
            }
            return sb.toString();
        } catch (Exception e) {
                e.printStackTrace();
        }
        
        return "";
  }
  
  /**
   * Get Root
   * @return
   */
  //Función con la cual se adquiere la raíz de merkle a partir de las transacciones de bloque  
  public String getRoot() {
    return this.root;
  }
    
}


// Sacado de http://java-lang-programming.com/en/articles/29