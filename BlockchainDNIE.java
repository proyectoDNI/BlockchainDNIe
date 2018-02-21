
package blockchaindni.e;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BlockchainDNIE {
    //Lista de bloques
    public static List<Block> Chain = new ArrayList<>();
    // Prueba de trabajo número de ceros iniciales
    public static  int NumVer=6;
    // Tiempo mínimo para completar la prueba de trabajo 
    private static final int tmin=90;
    // Tiempo máximo en el que se debe completar la prueba de trabajo
    private static final int tmax=150;
    public static Block BloqueActual;
    // Variable para calcular el espacio ocupado del bloque en bytes
    public static int space;
    
    /*Está función se encarga de calcula el hash del bloque respecto a la
    dificultad definida (número de ceros iniciales), además toma el tiempo en el
    que se demora en calcular dicho hash (el cual retorna como resultado de la función)*/
    public static int PruebaTrabajo(){
        Date aux=new Date();
        int ti=(int)(aux.getTime()/1000);
        String Verificar="";
        for (int i=0; i<NumVer;i++){
            Verificar=Verificar+"0";
        }
        int tiempo =((int)(aux.getTime()/1000))-ti;
        while (!BloqueActual.HHash.startsWith(Verificar)){
            BloqueActual.Nonce++;
            BloqueActual.HHash=BloqueActual.calcularhash();
            aux = new Date();
        }
        tiempo= ((int)(aux.getTime()/1000))-ti;
        System.out.println(tiempo+" s");
        return tiempo;
    }
    /*La función verifica el tiempo que se tomó para calcular el hash que cumpla
    con el reto y ajusta la dificultad si el tiempo es muy poco respecto al mínimo
    se aumenta la dificultad, sí el tiempo sobrepasa el máximo se disminuye la dificultad
    del reto. FALTA ENVIAR LA CORRECCION DEL RETO A LOS OTROS NODOS*/
    public static void Dificultad (int t){
        if (t < tmin){
            System.out.println("d");
            NumVer++;
            //enviar dificultad
        }
        if (t>tmax){
            System.out.println("t");
            NumVer--;//como corregir el reto
            //enviar dificultad
        }
    }
    /*Esta función verifica que el nonce y el bloque calcule el hash correcto 
    que cumpla con el reto, esta función sirve para la votación entre los nodos
    y aprobar que el bloque sea agregado al blockchain*/
    public static boolean ValidarNonce (int nonce){
        String Verificar="";
        for (int i=0; i<NumVer;i++){
            Verificar=Verificar+"0";
        }
        BloqueActual.Nonce=nonce;
        BloqueActual.HHash=BloqueActual.calcularhash();
        if (BloqueActual.HHash.startsWith(Verificar)){
            return true;
        }
        else {
            return false;
        }
    }
    /*Esta función luego de verificar que el bloque se correcto y sea votado por
    los nodos esta función agrega el bloque en el blockchain y incialica el 
    nuevo bloque en que se agregaran nuevas transacciones*/
    public static void AgregarBloque(){
        Chain.add(BloqueActual);
        System.out.println("Bloque Agregado\n\n");
        BloqueActual=new Block ( 0, 0, "", "0000000000000000000000000000000000000000000000000000000000000000");
        BloqueActual.Index=Chain.size();
        BloqueActual.PHash=Chain.get(Chain.size()-1).HHash;
        space=204;
    }
    /*Está función crea el primer bloque que conforma el blockchain con una
    información básica y un día ya especifico*/
    public static void BloqueGenesis (){
        System.out.println("Creando Bloque Genesis...");
        long x= 1511999287; //día de creación en formato unix
        x=x*1000;
        String info = "Republica de Colombia, Registaduria Nacional del Estado Civil, Documento de Identidad electronico, Cedula de Ciudadania";
        Date DiaCre = new Date(x);
        System.out.println(DiaCre);
        BloqueActual= new Block(0,(int) (DiaCre.getTime()/1000), info, "0000000000000000000000000000000000000000000000000000000000000000");
        BloqueActual.HHash=BloqueActual.calcularhash();
        int t = PruebaTrabajo();
        if (ValidarNonce(BloqueActual.Nonce)){
            Dificultad(t);
            AgregarBloque();
        }
    }
    /*Está función imprime el blockchain cada bloque agregado que tenga 
    almacenado el nodo que haga uso de esta función*/
    public static void ImprimirBlockchain(){
        System.out.println("---BLOCKCHAIN---");
        for (Block aux : Chain) {
            if (aux.Index==0)
                System.out.println("Bloque Genesis: ");
                System.out.println("    Bloque: "+aux.Index);
                System.out.println("    Hash: "+aux.HHash);
                System.out.println("    Hash Anterior: "+aux.PHash);
                System.out.println("    Nonce: "+aux.Nonce);
                System.out.println("    Timestamp: "+aux.TimeStamp);
                System.out.println("    Raiz Merkel: "+aux.MerkleRoot);
                System.out.println("    Transacciones: "+aux.Transacciones);
                //System.out.println(aux.MerkleTree);
                System.out.println("\n");
        }
    }
    /*Está función agrega transacciones al bloque, y actualiza el árbol de 
    merkle de bloque y el merkle root, y además aumenta la variable que calcula
    el espacio que ocupa el bloque en bytes*/
    public static boolean AgregarTransaccion(String info){
        //validar la transaccion
        BloqueActual.Transacciones.add(info);
        space=space+info.length();
        MerkleTrees merkleTrees = new MerkleTrees(BloqueActual.Transacciones);
        merkleTrees.merkle_tree();
        BloqueActual.MerkleRoot=merkleTrees.getRoot();
        BloqueActual.MerkleTree=merkleTrees.MerkleTree;
        return true; 
    }
    /*Crea la base de datos y la tabla en la cual se almacenará el blockchain
    por medio de comandos sql*/
    public static void creaDB(){
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/","postgres","12345");
            stmt=c.createStatement();
            String sql = "CREATE DATABASE blockchain;";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
            System.out.println("Base de datos creada");
        } catch (ClassNotFoundException | SQLException ex) {
            System.err.println("Ya existe...");
        }
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/blockchain","postgres","12345");
            stmt=c.createStatement();
            String sql = "CREATE TABLE dnie (index INT PRIMARY KEY, hhash TEXT, phasd TEXT, timestamp INT, nonce INT, merkleroot TEXT, transacciones TEXT);";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
            System.out.println("Tabla creada");
        } catch (ClassNotFoundException | SQLException ex) {
            System.err.println("Tabla ya existe...");
        }
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/blockchain","postgres","12345");
            stmt=c.createStatement();
            String sql = "CREATE TABLE chain (idnum INT PRIMARY KEY, compress BYTEA, tam INT);";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
            System.out.println("Tabla creada");
        } catch (ClassNotFoundException | SQLException ex) {
            System.err.println("Tabla ya existe...");
        }
    }
    /*borrar por medio de comandos sql el último registro en las tablas que 
    guardan el blockchain, para almacenar una nueva versión*/
    public static void BorrarUltimoRegistro(){
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/blockchain","postgres","12345");
            stmt=c.createStatement();
            String sql = "DELETE FROM dnie;";
            stmt.executeUpdate(sql);
            sql = "DELETE FROM chain;";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
            System.out.println("Ultimo Registro Borrado");
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(BlockchainDNIE.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /*Está función guarda la nueva versión del blockchain o la versión 
    actualizada del blockchain en las dos tablas creadas en la base de datos.
    Una guarda el blockchain sin comprimir cada fila es un bloque, y la otra guarda un archivo
    de bytes en el cual se encuentra comprimido el blockchain (por medio de comando sql)*/
    public static void GuardarBlockchain(){
        BorrarUltimoRegistro();
        Connection c = null;
        PreparedStatement preparedStmt= null;
        String Cadena=null;
        String ChainText=null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/blockchain","postgres","12345");
            String sql = "INSERT INTO dnie (index, hhash, phasd, timestamp, nonce, merkleroot, transacciones) VALUES (?, ?, ?, ?, ?, ?, ?);";
            preparedStmt = c.prepareStatement(sql);
            for (Block aux : Chain) {
                preparedStmt.setInt(1, aux.Index);
                preparedStmt.setString(2, aux.HHash);
                preparedStmt.setString(3, aux.PHash);
                preparedStmt.setInt(4, aux.TimeStamp);
                preparedStmt.setInt(5, aux.Nonce);
                preparedStmt.setString(6, aux.MerkleRoot);
                preparedStmt.setString(7, String.join(";",aux.Transacciones));
                preparedStmt.execute();
                Cadena=Integer.toString(aux.Index)+"%"+aux.HHash+"%"+aux.PHash+"%"+Integer.toString(aux.TimeStamp)+"%"+Integer.toString(aux.Nonce)+"%"+aux.MerkleRoot+"%"+String.join(";",aux.Transacciones);
                if (aux.Index==0){
                    ChainText=Cadena;
                }
                else{
                    ChainText=ChainText+"@"+Cadena;
                }
            }
            sql = "INSERT INTO chain (idnum, compress, tam) VALUES (?, ?, ?);";
            preparedStmt = c.prepareStatement(sql);
            preparedStmt.setInt(1, 0);
            preparedStmt.setBytes(2, compress(ChainText));
            preparedStmt.setInt(3, compress(ChainText).length);
            preparedStmt.execute();
            preparedStmt.close();
            c.close();
            System.out.println("Blockchain Almacenado");
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            Logger.getLogger(BlockchainDNIE.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /*Carga el blockchain almaceno en el database, la tabla con los bloques sin comprimir*/
    public static void CargarBlockchain(){
        Connection c = null;
        Statement stmt = null;
        Block aux = new Block ( 0, 0, "", "0000000000000000000000000000000000000000000000000000000000000000");
        String cadena[]=null;
        MerkleTrees merkleTrees=null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/blockchain","postgres","12345");
            c.setAutoCommit(false);
            stmt=c.createStatement();
            String sql = "SELECT * FROM dnie;";
            ResultSet rs=stmt.executeQuery(sql);
            while (rs.next()){
                aux.Index=rs.getInt("index");
                aux.HHash=rs.getString("hhash");
                aux.PHash=rs.getString("phasd");
                aux.TimeStamp=rs.getInt("timestamp");
                aux.Nonce=rs.getInt("nonce");
                aux.MerkleRoot=rs.getString("merkleroot");
                cadena=rs.getString("transacciones").split(";");
                aux.Transacciones.addAll(Arrays.asList(cadena));
                if(aux.Index==0){
                    aux.MerkleTree=null;
                }
                else{
                    merkleTrees = new MerkleTrees(aux.Transacciones);
                    merkleTrees.merkle_tree();
                    aux.MerkleTree=merkleTrees.MerkleTree;
                }
                Chain.add(aux);
                aux = new Block ( 0, 0, "", "0000000000000000000000000000000000000000000000000000000000000000");
            }
            stmt.close();
            c.close();
            System.out.println("Blockchain Cargado\n\n");
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(BlockchainDNIE.class.getName()).log(Level.SEVERE, null, ex);
        }
        BloqueActual=new Block ( 0, 0, "", "0000000000000000000000000000000000000000000000000000000000000000");
        BloqueActual.Index=Chain.size();
        BloqueActual.PHash=Chain.get(Chain.size()-1).HHash;
        space=204;
    }
    //Función que permite comprimir el blockchain luego de volver un archivo de texto plano y lo vuelve un arreglo de bytes
    public static byte[] compress(String data) throws IOException {
        byte[] compressed;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length())) {
            try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
                gzip.write(data.getBytes());
            }
            compressed = bos.toByteArray();
        }
	return compressed;
    }
    //Función que descomprime el blockchain del arreglo de bytes y lo vuelve un string que luego permite formar la cadena de bloques para almacenarla en la lista de clase block
    public static String decompress(byte[] compressed) throws IOException {
        StringBuilder sb;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed); GZIPInputStream gis = new GZIPInputStream(bis); BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"))) {
            sb = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) {
                sb.append(line);
            }          }
	return sb.toString();
    }
    /*Carga el blockchain almaceno en el database, la tabla con blockchain comprimido*/
    public static void CargarBlockchaincomprimido(){
        Connection c = null;
        Statement stmt = null;
        String CompressChain=null;
        String cadena[]=null;
        String Bloque[]=null;
        MerkleTrees merkleTrees=null;
        Block aux = new Block ( 0, 0, "", "0000000000000000000000000000000000000000000000000000000000000000");
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/blockchain","postgres","12345");
            c.setAutoCommit(false);
            stmt=c.createStatement();
            String sql = "SELECT compress FROM chain WHERE idnum=0";
            ResultSet rs=stmt.executeQuery(sql);
            if (rs.next()){
                CompressChain=decompress(rs.getBytes("compress"));
                cadena=CompressChain.split("@");
                for (String block : cadena) {
                    Bloque=block.split("%");
                    aux.Index=Integer.parseInt(Bloque[0]);
                    aux.HHash=Bloque[1];
                    aux.PHash=Bloque[2];
                    aux.TimeStamp=Integer.parseInt(Bloque[3]);
                    aux.Nonce=Integer.parseInt(Bloque[4]);
                    aux.MerkleRoot=Bloque[5];
                    aux.Transacciones.addAll(Arrays.asList(Bloque[6].split(";")));
                    if(aux.Index==0){
                    aux.MerkleTree=null;
                    }
                    else{
                        merkleTrees = new MerkleTrees(aux.Transacciones);
                        merkleTrees.merkle_tree();
                        aux.MerkleTree=merkleTrees.MerkleTree;
                    }
                    Chain.add(aux);
                    aux = new Block ( 0, 0, "", "0000000000000000000000000000000000000000000000000000000000000000");
                }
            }
            stmt.close();
            c.close();
            System.out.println("Blockchain Cargado\n\n");
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            Logger.getLogger(BlockchainDNIE.class.getName()).log(Level.SEVERE, null, ex);
        }
        BloqueActual=new Block ( 0, 0, "", "0000000000000000000000000000000000000000000000000000000000000000");
        BloqueActual.Index=Chain.size();
        BloqueActual.PHash=Chain.get(Chain.size()-1).HHash;
        space=204;
    }
    
    public static void main(String[] args){
        //      Crear Blqoue Genesis
        creaDB();
        System.out.println("\n");
        BloqueGenesis();
        //      Cargar Blockchain Almacenado
        //CargarBlockchaincomprimido();
        //CargarBlockchain();
        //      Bloques Posteriores
        String [] tran = new String [] {"ID:1019090511 Serial:0000000001 Clase:Notaria Tipo:EstadoCivil Datos:Casado",
        "ID:1019090512 Serial:0000000002 Clase:Impuestos Tipo:RegistroRUT Datos:8299",
        "ID:1019090513 Serial:0000000003 Clase:Gobierno Tipo:Subsidio Datos:Vivienda",
        "ID:1019090514 Serial:0000000005 Clase:Registraduria Tipo:Duplicado Datos:0000000004"};
        Date diaTran=null;
        int t;
        NumVer=5;
        //      BLOQUES POSTERIORES
        int i=0;
        while (i<4){
            //Recibir Transaccion
            if (AgregarTransaccion(tran[i])){
                diaTran=new Date();
                BloqueActual.TimeStamp=(int) (diaTran.getTime()/1000);
                BloqueActual.HHash=BloqueActual.calcularhash();
            }
            if (space>=32000){
                System.out.println("Bloque: "+BloqueActual.Index);
                System.out.println(diaTran);
                System.out.println(space+" --> "+BloqueActual.Transacciones.size());
                t=PruebaTrabajo();
                if (ValidarNonce(BloqueActual.Nonce)){
                    Dificultad(t);
                    AgregarBloque();
                }
                i++;
            }
        }
        //    IMPRIMIR EL BLOCKCHAIN
        ImprimirBlockchain();
        //    Guardar Blokchain
        GuardarBlockchain();
    }
}