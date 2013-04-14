import java.awt.List;
import java.io.*;
import java.util.*;
import java.lang.*;



/*
 * Author: Carlos Reyes
 * 
 * Cashesim - A simple cache simulator
 * 
 */

public class CachesimEngine{

	//Creating the object representing a cache data slot
	public class CacheSpace{
		public String data = null;
		public int validBit = 0;
		public int index;
		public int tag;
		public int set;
		public int offSet;

		@Override
		public boolean equals(Object obj){
			if(this == obj){
				return true;
			}
			if (!(obj instanceof CacheSpace)){
				return false; 
			}
			CacheSpace slot = (CacheSpace)obj;
			return tag == slot.tag; 
		}
		//public float lastReadTime;
	}


	//Setting up all of the constants
	private int cacheSize = ( Integer.parseInt(argList.get(1)) )*1048576; //Size of cache in bytes
	private int setAssociation = Integer.parseInt(argList.get(2));
	private int blockSize = Integer.parseInt(argList.get(3));
	private int numBlocks = cacheSize/blockSize;
	private int numSets = numBlocks/setAssociation;
	private static HashMap<String, String> map = new HashMap<String, String>();

	//Cast inputs to doubles (for use with Java Math library)
	double bSize = (double) blockSize;
	double nSets = (double) numSets;

	//Calculating the size of each element of the address
	double oBits = Math.log10(bSize)/Math.log10(2);
	double sBits = Math.log10(nSets)/Math.log10(2);

	int offsetBits = (int) Math.round(oBits);
	int setBits = (int) Math.round(sBits);
	int tagBits = 24-setBits-offsetBits;



	//Initializing hard-coded hashmap
	static{
		map.put("0", "0000");
		map.put("1", "0001");
		map.put("2", "0010");
		map.put("3", "0011");
		map.put("4", "0100");
		map.put("5", "0101");
		map.put("6", "0110");
		map.put("7", "0111");
		map.put("8", "1000");
		map.put("9", "1001");
		map.put("a", "1010"); //10
		map.put("b", "1011"); //11
		map.put("c", "1100"); //12
		map.put("d", "1101"); //13
		map.put("e", "1110"); //14
		map.put("f", "1111"); //15
	}

	//Cache data structures
	public static ArrayList<String> argList = new ArrayList<String>();
	//public CacheSpace[][] cache = new CacheSpace[numSets][setAssociation];
	public ArrayList<ArrayList<CacheSpace>> cache = new ArrayList<ArrayList<CacheSpace>>();
	public HashMap<String, String> memory = new HashMap<String, String>();

	//Loading the appropriate number of Cache spaces into arraylist
	//	public void setUp(){
	//		for(int i=0; i<numSets; i++){
	//			for(int j=0; j<setAssociation; j++){
	//				
	//			}
	//		}
	//	}





	//Reading through the input file
	public void readFile(){
		String name = argList.get(0);
		File f = new File(name);

		String initial = "";

		for(int i=0; i<(blockSize*2); i++){
			initial+="0"; // if 8 --> 0000 0000 0000 0000
		}

		//Initializing the "empty" arraylist (cache)
		CacheSpace testSlot = new CacheSpace();
		ArrayList<CacheSpace> money = new ArrayList<CacheSpace>();
		for(int k=0; k<setAssociation; k++){
			money.add(testSlot);
		}
		for(int i=0; i<numSets; i++){
			cache.add(money);
		}
		//System.out.println(cache);
		//End Initialize

		try {
			Scanner s = new Scanner(f);

			while(s.hasNextLine()){
				ArrayList<String> instructions = new ArrayList<String>();
				String instructionLine = s.nextLine();
				String[] parts = instructionLine.split(" ");
				for(int i = 0; i<parts.length; i++)
					instructions.add(parts[i]);
				String realInst = hexConverter(instructions.get(1)); //convert hex to binary string

				//breaking up the instruction code and turning binary numbers into ints
				int tagInt = binaryToTag(realInst);
				int setInt = binaryToSet(realInst);
				int offsetInt = binaryToOffset(realInst);

				//set counter
				int count = 0;

				//begin actual cache operations
				for(ArrayList<CacheSpace> set : cache){
					//CacheSpace slot = null;
					if(count==setInt){
						int counter = 0;
						for(CacheSpace slot : set){
							if((slot.tag)==(tagInt)){ //cache hit

								if ( instructions.get(0).equals("store") ){ //store hit
									//System.out.println(slot.data + "Here!");
									//do a write through
									String offSetZeros = "";
									for(int i=0; i<(binaryToOffset(realInst)); i++){
										offSetZeros += "00";
									}
									//writing to cache
									//slot.data = initial.substring(( ( Integer.parseInt(instructions.get(2)) )*2 )+(offsetBits*2)) + instructions.get(3); //putting the string data into the cache slot
									slot.data = initial.substring( ( Integer.parseInt(instructions.get(2)) )*2 + 2*(binaryToOffset(realInst))) + instructions.get(3) + offSetZeros;
									slot.validBit = 1; //ensuring that the slot's valid bit is 1
									slot.tag = binaryToTag(realInst);
									slot.set = binaryToSet(realInst);
									slot.offSet = binaryToOffset(realInst);

									//preparing to write to memory
									int valueOffset = binaryToOffset(realInst);
									char[] mapData = instructions.get(3).toCharArray();


									//pulling the old value at that key if it exists
									//if so, combine old and new, if not, make key-value pair
									if( memory.containsKey(realInst.substring(0, tagBits+setBits))){
										String old = memory.get(realInst.substring(0, tagBits+setBits));
										char[] charArray = old.toCharArray();
										int length = charArray.length;
										//System.out.println(charArray);
										//overwriting old string with new data
										for(int k=0; k<mapData.length; k++){
											if(mapData[k]!='0'){
												//System.out.println(charArray);
												//charArray[(k+(valueOffset))+6] = mapData[k];
												charArray[(k+ charArray.length - 2*Integer.parseInt(instructions.get(2)) - 2*binaryToOffset(realInst))] = mapData[k];
											}
											
										}
										String backToMem = new String(charArray);
										//System.out.println(backToMem.substring(backToMem.length()-(2*(Integer.parseInt(instructions.get(2))))));
										slot.data = backToMem;
										memory.put(realInst.substring(0, tagBits+setBits), backToMem);
										System.out.println("store hit");
										//System.out.println("After Store hit " + memory.get(realInst.substring(0, tagBits+setBits)));

										//System.out.println("store hit " + backToMem.substring(backToMem.length()-( 2*binaryToOffset(realInst) + 2*Integer.parseInt(instructions.get(2)) ), backToMem.length()- 2*binaryToOffset(realInst) ));

									}
									else{
										//putting the string data into the memory at key of tag+set in binary

										//int upTo = 24 - ( ( Integer.parseInt(instructions.get(2)) )*2 )+(offsetBits*2);
										String offSetZeros2 = "";
										for(int i=0; i<(binaryToOffset(realInst)); i++){
											offSetZeros2 += "00";
										}
										String newData = initial.substring( ( Integer.parseInt(instructions.get(2)) )*2 + 2*(binaryToOffset(realInst))) + instructions.get(3) + offSetZeros2;

										memory.put(realInst.substring(0, tagBits+setBits), newData);
									}

								}

								if ( instructions.get(0).equals("load") ){ //load hit
									//System.out.println("slot data " + slot.data);
									System.out.println("load hit " + (slot.data).substring((slot.data).length()-( 2*binaryToOffset(realInst) + 2*Integer.parseInt(instructions.get(2)) ), (slot.data).length()- 2*binaryToOffset(realInst) ));
									//System.out.println("This is where the data from memory goes");
								}
								continue;
							}

							//----------------------------------------------------------------

							if(counter==3 && (slot.tag)!=(tagInt)){ //cache miss
								//count=0; //reset counter
								if ( instructions.get(0).equals("store") ){ //store miss
									System.out.println("store miss");

									//preparing to write to memory
									int valueOffset = binaryToOffset(realInst);
									char[] mapData = instructions.get(3).toCharArray();

									//write the data straight to the memory
									if( memory.containsKey(realInst.substring(0, tagBits+setBits))){
										String old = memory.get(realInst.substring(0, tagBits+setBits));
										char[] charArray = old.toCharArray();
										//System.out.println(charArray);
										//overwriting old string with new data
										for(int k=0; k<mapData.length; k++){
											if(mapData[k]!='0'){
												charArray[k+( (charArray.length) - (valueOffset*4) )]= mapData[k];
											}
										}
										String backToMem = new String(charArray);
										memory.put(realInst.substring(0, tagBits+setBits), backToMem);
									}
									else{

										//make string of 0s corresponding to length of offset
										String offSetZeros = "";
										for(int i=0; i<(binaryToOffset(realInst)); i++){
											offSetZeros += "00";
										}
										String newData = initial.substring( ( Integer.parseInt(instructions.get(2)) )*2 + 2*(binaryToOffset(realInst))) + instructions.get(3) + offSetZeros;
										memory.put(realInst.substring(0, tagBits+setBits), newData);
									}


								}

								if ( instructions.get(0).equals("load") ){ //load miss
									//pulling data from memory
									if(! memory.containsKey(realInst.substring(0, tagBits+setBits))){
										String offSetZerosD = "";
										for(int i=0; i<Integer.parseInt(instructions.get(2)); i++){
											offSetZerosD += "00";
										}
										memory.put(realInst.substring(0, tagBits+setBits), initial);
										System.out.println("load miss " + offSetZerosD);

									}
									else{
									String toCache = memory.get(realInst.substring(0, tagBits+setBits));

									//make new CacheSpace object to put in cache
									CacheSpace cacheSlot = new CacheSpace();

									//putting memory in cache

									cacheSlot.data = toCache; // + initial.substring((offsetBits*2))
									cacheSlot.validBit = 1; //ensuring that the slot's valid bit is 1
									cacheSlot.tag = binaryToTag(realInst);
									cacheSlot.set = binaryToSet(realInst);
									cacheSlot.offSet = binaryToOffset(realInst);

									set.remove(setAssociation-1);
									set.add(0, cacheSlot);

									//System.out.println("load miss " + toCache);
//									int offSet =
//									int size =
//									in length = 
									
									System.out.println("load miss " + toCache.substring(toCache.length()-( 2*binaryToOffset(realInst) + 2*Integer.parseInt(instructions.get(2)) ), toCache.length()- 2*binaryToOffset(realInst) ));
									//System.out.println(toCache.substring(toCache.length()-( 2*binaryToOffset(realInst) + 2*Integer.parseInt(instructions.get(2)) ), toCache.length()- 2*binaryToOffset(realInst) ));

									}

								}
							}
						counter++;
						}
					}
					count+=1;

					//System.out.println(memory);

				}
			}

			//end cache operation loop


		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	} //end readfile

	public String hexConverter(String hex){
		String part1 = map.get(hex.substring(2, 3));
		String part2 = map.get(hex.substring(3, 4));
		String part3 = map.get(hex.substring(4, 5));
		String part4 = map.get(hex.substring(5, 6));
		String part5 = map.get(hex.substring(6, 7));
		String part6 = map.get(hex.substring(7, 8));

		String concat = part1+part2+part3+part4+part5+part6;
		//System.out.println(concat);
		return concat;
	}

	public int binaryToTag(String binary){
		//offsetBits, setBits, tagBits
		//tag
		String t = binary.substring(0, tagBits);
		int tag = Integer.parseInt(t, 2);
		return tag;
	}
	public int binaryToSet(String binary){
		//offsetBits, setBits, tagBits
		//set
		String s = binary.substring(tagBits, tagBits+setBits);
		int set = Integer.parseInt(s, 2);
		return set;
	}
	public int binaryToOffset(String binary){
		//offsetBits, setBits, tagBits	
		//offset
		String o = binary.substring(tagBits+setBits);
		int offSet = Integer.parseInt(o, 2);
		return offSet;
	}

	public static void main(String[] args){
		for(String s : args){
			argList.add(s);
		}

		CachesimEngine cachesim = new CachesimEngine();

		cachesim.readFile();

	}
}
