package booleanFormulaEntities;

/**
 * relationship between two range/regions
 * has relationship 
 * @author tingxie
 *
 */
public enum RegionRelationship {
Overlap, //[1,3] and [2,4]
NonOverLapNoMeet, //[1,3] and [4,5]
NonOverLapButMeet, //[1,3] and [3,4]
NotComparable, 
Contained, // [1,3] and [1,4]
Contains; //[1,3] and [1,1]
}
