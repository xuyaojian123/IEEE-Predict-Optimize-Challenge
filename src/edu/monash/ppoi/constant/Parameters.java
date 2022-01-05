package edu.monash.ppoi.constant;

import edu.monash.ppoi.checker.DateHandler;

import java.time.*;

/**
 *	Modify these paths to align with your file system's location of your data files.
 */
public class Parameters {
//	public static final ZoneId sourceTZ = ZoneId.of("UTC");
//	public static final ZoneId targetTZ = ZoneId.of("Australia/Queensland");
	
//	public static String priceFile = "data/PRICE_AND_DEMAND_202010_VIC1.csv";
//	public static String validationTSF = "data/prediction_data.tsf";
	
	
	public static final int year = 2020;
	public static final Month month = Month.NOVEMBER;
	public static final int DAYS = month.length(Year.isLeap(year));
	
	public static final LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
	public static final Duration duration = Duration.ofDays(month.length(Year.isLeap(year)));
	public static final int timeSlotsLength = DAYS * DateHandler.PERIODS_PER_DAY;
	
	public static final int WORKINGDAY = 5; // ÐÇÆÚ1µ½ÐÇÆÚ5
	
	public static final int maxBoundR = 32/2;
	public static final int maxBoundA = 96/2;


}
