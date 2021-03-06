/*
 * The purpose of this class is to process the inputted baby name data and offer methods for analyzing
 * the data and providing useful information about it (requested by the assignment questions).
 * I spent a lot of time considering the design of this code and have had a hard time finding places for
 * improvement. I believe it is well designed because I made a great effort to eliminate any duplication
 * contained within it. Likewise, I believe it is readable due to its use of clear variable names, no magic
 * values, and simple, single-purpose methods; I have also made some changes to improve this readability.
 * Finally, it is easy to implement features due to the inclusion of simple methods that can be used as
 * "pieces" for more complex features. While I have had trouble determining what to improve, I know that my
 * code is certainly not perfect and would value feedback to help me as I move forward!
 */


package names;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

// this class represents the PROCESSING stage of IPO
public class NameCollector {

    public static final String MALE = "M";
    public static final String FEMALE = "F";
    // ascii code 97 is 'a'; by subtracting this from a char value, we can convert chars to ints in range [0,25],
    // and by adding this conversion value to an int in the same range, we can convert ints to the characters 'a' to 'z'
    public static final int CHAR_TO_INT_CONVERSION_VALUE = 97;

    // maps from sex to map of year to map of name to popularity
    private HashMap<String, HashMap<Integer, HashMap<String, Integer>>> names = new HashMap<>();
    // maps from sex to map of year to list of names ordered by rank
    private HashMap<String, HashMap<Integer, List<String>>> rankedNames = new HashMap<>();

    private int mostRecentYear = Integer.MIN_VALUE;
    private int earliestYear = Integer.MAX_VALUE;

    public NameCollector(List<String> yearNameFiles) {
        putSexKeysIntoMap(names);
        putSexKeysIntoMap(rankedNames);
        for (String yearNameFile : yearNameFiles) {
            int year = extractYear(yearNameFile);
            initializeNameMap(year);
            if (year > mostRecentYear) mostRecentYear = year;
            if (year < earliestYear) earliestYear = year;
            scanFile(yearNameFile, year);
        }
        rankNames();
    }

    public int getPopularityForYear(String sex, int year, String name) {
        return names.get(sex).get(year).get(name);
    }

    public List<Integer> getRankForRangeOfYears(String sex, String name, int start, int end) {
        List<Integer> popularityList = new ArrayList<>();
        System.out.println("Rank for " + name + ", " + sex + " from " + start + " to " + end + ":");
        for (int year = start; year <= end; year++) {
            int rank = getRankBySexYearAndName(sex, year, name);
            popularityList.add(rank);
            System.out.println(year + ": " + rank);
        }
        System.out.println("\n");
        return popularityList;
    }

    public List<Integer> getRankForAllYears(String sex, String name) {
        return getRankForRangeOfYears(sex, name, earliestYear, mostRecentYear);
    }

    public String getMatchingRankInMostRecentYear(String sex, int year, String name) {
        // subtract 1 to get rank index
        int rankIndex = getRankBySexYearAndName(sex, year, name) - 1;
        String match = getNameBySexYearAndRank(sex, mostRecentYear, rankIndex);
        System.out.println("The name in the most recent year (" + mostRecentYear + ") with same rank (" + (rankIndex+1) +
                        ") as " + name + ", " + sex + " in " + year + " is:\n" + match + "\n");
        return match;
    }

    public List<String> getMostFrequentTopRankedNameInRangeOfYears(String sex, int start, int end) {
        Map<String, Integer> frequencyMap = new HashMap<>();
        for (int year = start; year <= end; year++) {
            String topRank = rankedNames.get(sex).get(year).get(0);
            frequencyMap.putIfAbsent(topRank, 0);
            frequencyMap.put(topRank, frequencyMap.get(topRank)+1);
        }

        List<String> mostFrequentTopRanks = new ArrayList<>();
        List<String> topRanks = new ArrayList<>(frequencyMap.keySet());
        Collections.sort(topRanks, Comparator
                .comparing((String name) -> frequencyMap.get(name))
                .reversed()
                .thenComparing((String name) -> name));
        int highestFrequency = 0;
        for (String topRank : topRanks) {
            if (frequencyMap.get(topRank) >= highestFrequency) {
                highestFrequency = frequencyMap.get(topRank);
                mostFrequentTopRanks.add(topRank);
            }
            else break;
        }

        Collections.sort(mostFrequentTopRanks);
        System.out.println("Between " + start + " and " + end + ", the following name(s) held the top rank for "
                + highestFrequency + " years.");
        printAllCollectionElements(mostFrequentTopRanks);
        return mostFrequentTopRanks;
    }

    public Set<String> getMostPopularStartingLetterForSexInRangeOfYears(String sex, int start, int end) {
        int [] tracker = new int [26];
        Map<Character, Set<String>> mapOfStartingLetterToNames = new HashMap<>();
        for (int year = start; year <= end; year++) {
            for (String name : names.get(sex).get(year).keySet()) {
                char startingLetter = name.toLowerCase().charAt(0);
                tracker[startingLetter- CHAR_TO_INT_CONVERSION_VALUE] += getPopularityForYear(sex, year, name);
                mapOfStartingLetterToNames.putIfAbsent(startingLetter, new TreeSet<>(Comparator.comparing((String nameAdded) -> nameAdded)));
                mapOfStartingLetterToNames.get(startingLetter).add(name);
            }
        }
        char mostPopularLetter = '.';
        int highestCount = -1;
        for (int i = 0; i < tracker.length; i++) {
            if (tracker[i] > highestCount) {
                mostPopularLetter = (char) (CHAR_TO_INT_CONVERSION_VALUE +i);
                highestCount = tracker[i];
            }
        }

        System.out.println("Between " + start + " and " + end + ", the following name(s) of sex " + sex
                + " had the most popular starting letter.");
        printAllCollectionElements(mapOfStartingLetterToNames.get(mostPopularLetter));
        return mapOfStartingLetterToNames.get(mostPopularLetter);
    }

    public int getDifferenceInRankBetweenTwoYearsForNameAndSex(String sex, String name, int start, int end) {
        return getRankBySexYearAndName(sex, end, name) - getRankBySexYearAndName(sex, start, name);
    }

    public int getDifferenceInRankBetweenFirstAndLastYearsForNameAndSex(String sex, String name, boolean printDifference) {
        int difference = getDifferenceInRankBetweenTwoYearsForNameAndSex(sex, name, earliestYear, mostRecentYear);
        if (printDifference) System.out.println("The difference in rank is: " + difference);
        return difference;
    }

    public int getNameWithGreatestDifferenceInRankBetweenTwoYears(int start, int end) {
        // key will be in form name,sex (e.g. "Pierce,M")
        Map<String, Integer> differenceMap = new HashMap<>();

        List<String> startYearNames = new ArrayList<>();
        for (String sex : List.of(MALE, FEMALE)) {
            for (String name : names.get(sex).get(start).keySet()) {
                startYearNames.add(name + "," + sex);
            }
        }

        for (String sex : List.of(MALE, FEMALE)) {
            for (String name : names.get(sex).get(end).keySet()) {
                // check if name has rank in both years to avoid error
                if (startYearNames.contains(name + "," + sex)) {
                    int difference = getDifferenceInRankBetweenTwoYearsForNameAndSex(sex, name, start, end);
                    differenceMap.put(name + "," + sex, difference);
                }
            }
        }

        int maxDifference = Integer.MIN_VALUE;
        for (int difference : differenceMap.values()) {
            if (Math.abs(difference) > Math.abs(maxDifference)) maxDifference = difference;
        }

        System.out.println("The following name(s) had the greatest difference in rank (" + maxDifference + ")");
        for (String name : differenceMap.keySet()) {
            if (differenceMap.get(name) == maxDifference) System.out.println(name);
        }
        return maxDifference;
    }

    public int getAverageRankInRangeOfYears(String sex, String name, int start, int end) {
        // we add 1 since the range is inclusive (so if it is 2000 to 2000, we have 1 year, not 0)
        int numberOfRanks = end - start + 1;
        int totalRanks = 0;
        for (int year = start; year <= end; year++) {
            totalRanks += getRankBySexYearAndName(sex, year, name);
        }
        // rounded down (e.g. 1.8 is rank 1, not 2)
        int averageRank = totalRanks/numberOfRanks;
        System.out.println("Average rank:" + averageRank);
        return averageRank;

    }

    private int getRankBySexYearAndName(String sex, int year, String name) {
        List<String> ranksBySexAndYear = rankedNames.get(sex).get(year);
        int rank = ranksBySexAndYear.indexOf(name);
        if (rank == -1) {
            try {
                throw new Exception();
            } catch (Exception e) {
                System.out.println("Name \"" + name + "\" with sex " + sex + " not present in data for year " + year + ".\n");
                e.printStackTrace();
            }
        }
        return rank+1;
    }

    private String getNameBySexYearAndRank(String sex, int year, int rank) {
        String name = "invalid";
        try {
            name =  rankedNames.get(sex).get(year).get(rank);
        }
        catch (IndexOutOfBoundsException e) {
            System.out.println("No name for rank " + rank + " in year " + year + " for sex " + sex + ".\n");
        }
        return name;
    }

    private void rankNames() {
        for (String sex : names.keySet()) {
            for (int year : names.get(sex).keySet()) {
                // rank the names first by popularity and then, in the case of ties, alphabetically
                List<String> ranksBySexAndYear = new ArrayList<>();
                HashMap<String, Integer> namesToPopularityMap = names.get(sex).get(year);
                Set<String> names = namesToPopularityMap.keySet();
                for (String name : names) {
                    ranksBySexAndYear.add(name);
                }
                Collections.sort(ranksBySexAndYear,Comparator
                        .comparing((String name) -> getPopularityForYear(sex, year, name))
                        .reversed()
                        .thenComparing((name) -> name));
                rankedNames.get(sex).put(year, ranksBySexAndYear);
            }
        }
    }

    private void putSexKeysIntoMap(HashMap map) {
        map.put(MALE, new HashMap<>());
        map.put(FEMALE, new HashMap<>());
    }

    private int initializeNameMap(int year) {
        names.get(MALE).put(year, new HashMap<>());
        names.get(FEMALE).put(year, new HashMap<>());
        return year;
    }

    private int extractYear(String yearNameFile) {
        String [] slashSplit = yearNameFile.split("/");
        return Integer.parseInt(slashSplit[slashSplit.length - 1].split("\\.")[0].split("yob")[1]);
    }

    private void printAllCollectionElements(Collection collection) {
        for (Object obj : collection) System.out.println(obj);
        System.out.println("\n");
    }

    private void scanFile(String yearNameFile, int year) {
        try {
            File file = new File(yearNameFile);
            Scanner myReader = new Scanner(file);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String [] cleanRow = data.split(",");
                String sex = cleanRow[1]; String name = cleanRow[0];
                int popularity = Integer.parseInt(cleanRow[2]);
                names.get(sex).get(year).put(name, popularity);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred while reading name file: " + yearNameFile);
            e.printStackTrace();
        }
    }
}
