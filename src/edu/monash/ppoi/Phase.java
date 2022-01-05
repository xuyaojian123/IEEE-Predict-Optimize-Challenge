package edu.monash.ppoi;

import edu.monash.io.FileUtils;
import edu.monash.io.tsf.PlainTimeSeries;
import edu.monash.io.tsf.PricePlainTimeSeries;
import edu.monash.io.tsf.TimeSeriesDB;
import edu.monash.io.tsf.WeatherPlainTimeSeries;
import edu.monash.ppoi.checker.*;
import edu.monash.ppoi.instance.Instance;
import edu.monash.ppoi.solution.Schedule;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public enum Phase {

	/*
	 * Connect phase with month of the year and input testing data file.
	 * Note that phase_2_data.tsf will be released once phase 1 is complete.
	 */
	P0(2020, Month.SEPTEMBER, "phase_1_data.tsf"),
	P1(2020, Month.OCTOBER,   "phase_2_data.tsf"),
	P2T(2020, Month.OCTOBER,  "phase_2_data.tsf"),
	P2(2020, Month.NOVEMBER,  "phase_3_data.tsf");

	/*
	 * Root folder where data files are stored; adjust according to local paths.
	 */
	private static final String ROOT = "data/";

	private static final String PRICE_FORMAT = "electricity_price/PRICE_AND_DEMAND_%4d%02d_VIC1.csv";
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	private static final ZoneId SOURCE_TZ = ZoneId.of("UTC");
	private static final ZoneId TARGET_TZ = ZoneId.of("Australia/Queensland");

	private final int year;
	private final Month month;
	private final String tsfFile;

	private Phase(int year, Month month, String tsfFile) {
		this.year = year;
		this.month = month;
		this.tsfFile = tsfFile;
	}

	public int getYear() {
		return year;
	}

	public Month getMonth() {
		return month;
	}

	public TimeSeriesDB getLoadSeries() {
		return TimeSeriesDB.parse(new File(getTSFFile()));
	}

	public List<Double> getPrices() {
		return FileUtils.readPriceCSV(getPriceFile());
	}

	public DateHandler getDateHandler() {
		return new DateHandler(year, month);
	}

	public String getPriceFile() {
		return String.format(ROOT + PRICE_FORMAT, year, month.getValue());
	}

	public String getTSFFile() {
		return ROOT + tsfFile;
	}

	public LocalDateTime getStart() {
		return LocalDateTime.of(year, month, 1, 0, 0);
	}

	public Duration getDuration() {
		return Duration.ofDays(month.length(Year.isLeap(year)));
	}

	public void checkSchedule(String[] args) {
		try {
			System.out.println(getObjective(args[0], args[1]));
		} catch (Exception ex) {
			System.out.println(Double.NaN);
			System.out.flush();

			if (args.length != 2) {
				System.err.println("Expected two input arguments, instance file and schedule file.");
			} else {
				System.err.println("Error while evaluating schedule.");
				ex.printStackTrace();
			}
		}
	}

	public double getObjective(String instanceFilename, String scheduleFilename) {
		return getEvaluator().getScheduleChecker(instanceFilename, scheduleFilename).getObjective();
	}

	public IEvaluator getEvaluator() {
		if (this.equals(Phase.P2) || this.equals(Phase.P2T)) {
			return new ChronicsEvaluator(new ChronicsHandler(year, month, getLoadSeries(), readPriceSeries()));
		} else {
			return new Evaluator(getLoadSeries(), getPrices(), getDateHandler());
		}
	}

	public static PlainTimeSeries readPriceSeries() {

		// Read data from disk.
		List<String[]> rows = concatenatePriceFiles();

		// Concatenate the three price files as AEST time series.
		PlainTimeSeries catSeries = PlainTimeSeries.fromRows(rows, 1, 3, DATE_FORMAT, TARGET_TZ);

		// Convert to UTC time, shift down 30 mins to start of the period, upsample so the delta is 15 mins.
		PlainTimeSeries fullSeries = catSeries.withZoneSameInstant(SOURCE_TZ)
											  .minus(Duration.ofMinutes(30))
											  .fillForward(Duration.ofMinutes(15));

		return fullSeries;
	}

	public static List<String[]> concatenatePriceFiles() {

		List<String> files = new ArrayList<>();
		
		for (int month = 9; month <= 12; month++) 
			files.add(ROOT + String.format(PRICE_FORMAT, 2020, month));

		return FileUtils.readCSVs(files);
	}

	public interface IEvaluator {
		public IScheduleChecker getScheduleChecker(String instanceFilename, String scheduleFilename);
	}

	public static class Evaluator implements IEvaluator {

		private final TimeSeriesDB db;
		private final List<Double> prices;
		private final DateHandler dates;

		public Evaluator(TimeSeriesDB db, List<Double> prices, DateHandler dates) {
			this.db = db;
			this.prices = prices;
			this.dates = dates;
		}

		public IScheduleChecker getScheduleChecker(String instanceFilename, String scheduleFilename) {

			// Read in the instance
			Instance instance = Instance.parseInstance(new File(instanceFilename));
			Schedule schedule = Schedule.parseSchedule(new File(scheduleFilename), instance);

			return new ScheduleChecker(db, schedule, prices, dates);
		}
	}

	public static class ChronicsEvaluator implements IEvaluator {

		private final ChronicsHandler db;

		public ChronicsEvaluator(ChronicsHandler db) {
			this.db = db;
		}

		public IScheduleChecker getScheduleChecker(String instanceFilename, String scheduleFilename) {

			// Read in the instance
			Instance instance = Instance.parseInstance(new File(instanceFilename));
			Schedule schedule = Schedule.parseSchedule(new File(scheduleFilename), instance);

			return new ChronicsScheduleChecker(db, schedule);
		}
	}

	/**
	 * @create and @modify by 徐耀建
	 * 用来提取价格数据 2019年8月-2020年10月(价格数据从AEST转成UTC)
	 * @return
	 */
	public static PricePlainTimeSeries readPriceSeries_AEST_to_UTC() {

		// Read data from disk.
		List<String[]> rows = concatenatePriceFiles_AEST_to_UTC();

		// Concatenate the three price files as AEST time series.
		PricePlainTimeSeries catSeries = PricePlainTimeSeries.fromRows(rows, 1, DATE_FORMAT, TARGET_TZ);


		// Convert to UTC time, shift down 30 mins to start of the period, upsample so the delta is 15 mins.
		PricePlainTimeSeries fullSeries = catSeries.withZoneSameInstant(SOURCE_TZ)
				.minus(Duration.ofMinutes(30))
				.fillForward(Duration.ofMinutes(15));

		return fullSeries;
	}

	/**
	 * @create and @modify by 徐耀建
	 * 用来提取价格数据 2019年8月-2020年10月(价格数据从AEST转成UTC)
	 * @return
	 */
	public static List<String[]> concatenatePriceFiles_AEST_to_UTC() {

		List<String> files = new ArrayList<>();

		for (int month = 8; month <= 12; month++){
			files.add(ROOT + String.format(PRICE_FORMAT, 2019, month));
		}
		for (int month = 1; month <= 12; month++)
			files.add(ROOT + String.format(PRICE_FORMAT, 2020, month));

		return FileUtils.readCSVs(files);
	}

	/**
	 * @create and @modify by 徐耀建
	 * 用来提取天气数据
	 * @param filePath 天气文件的路径地址
	 * @return
	 */
	public static WeatherPlainTimeSeries readWeatherSeries_UTC(String filePath) {

		// Read data from disk.
		List<String[]> rows = FileUtils.readWeatherCSV(filePath);


		// weather files as UTC time series.
		WeatherPlainTimeSeries weatherPlainTimeSeries = WeatherPlainTimeSeries.fromRows(rows, 0, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), SOURCE_TZ);

		// shift down 45 mins to start of the period, upsample so the delta is 15 mins.
		WeatherPlainTimeSeries fullSeries = weatherPlainTimeSeries.minus(Duration.ofMinutes(45))
				.fillForward(Duration.ofMinutes(15));

		return fullSeries;
	}
}
