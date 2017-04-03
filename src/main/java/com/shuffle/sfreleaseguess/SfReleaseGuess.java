package com.shuffle.sfreleaseguess;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.enumeration.SearchType;
import com.omertron.themoviedbapi.model.movie.MovieInfo;
import com.omertron.themoviedbapi.results.ResultList;

public class SfReleaseGuess {

	private final static transient Log log = LogFactory.getLog(SfReleaseGuess.class);

	private TheMovieDbApi theMovieDbApi;

	private Pattern patternMovieNameAndYear = Pattern.compile("(.*?)((\\d+)(?!.*(19|20)\\d{2}))");

	private Pattern patternCleanReleaseNaming = Pattern.compile(
			"((\\.|-)(db25|avc|ac3|dts|custom|dc|divx|divx5|dsr|dsrip|dutch|dvd|dvdrip|dvdscr|dvdscreener|screener|dvdivx|cam|fragment|fs|hdtv|hdrip|hdtvrip|internal|limited|multisubs|ntsc|ogg|ogm|pal|pdtv|proper|repack|rerip|retail|cd[1-9]|r3|r5|bd5|se|svcd|swedish|german|read.nfo|nfofix|unrated|ws|telesync|ts|telecine|tc|brrip|bdrip|480p|480i|576p|576i|720p|720i|1080p|1080i|hrhd|hrhdtv|hddvd|web-dl|bluray|x264|h264|xvid|xvidvd|xxx|www.www|DD5\\.1)|(-(.*)))",
			Pattern.CASE_INSENSITIVE);

	public SfReleaseGuess(String theMovieDbApiKey) {
		try {
			theMovieDbApi = new TheMovieDbApi(theMovieDbApiKey);
		} catch (MovieDbException e) {
			throw new RuntimeException(e);
		}
	}

	private MovieInfo guessByMovieNameAndYear(String release) {
		Matcher matcher = patternMovieNameAndYear.matcher(release);
		if (matcher.find()) {
			String movieTitle = matcher.group(1).replaceAll("(\\.|-|_)", " ").trim();
			int year = 0;

			try {
				year = Integer.valueOf(matcher.group(2));
			} catch (NumberFormatException e) {

			}
			log.debug(movieTitle);
			log.debug(year);
			if (StringUtils.isNotBlank(movieTitle)) {
				try {
					ResultList<MovieInfo> resultList = theMovieDbApi.searchMovie(movieTitle, 0, null, true, year, null, SearchType.NGRAM);
					if (resultList.getTotalResults() > 0) {
						MovieInfo movieInfo = resultList.getResults().get(0);
						log.info("Found " + movieInfo.getOriginalTitle());
						return movieInfo;
					} else {
						log.info("Not found");
					}
				} catch (MovieDbException e) {
					log.error("Error trying to guess movie by name and year", e);
				}
			}
		}
		return null;
	}

	// FIXME think better way to do this
	@SuppressWarnings("unused")
	private MovieInfo guessByCleanName(String release) {
		Matcher matcher = patternCleanReleaseNaming.matcher(release);
		if (matcher.find()) {
			String movieTitle = matcher.replaceAll("").replaceAll("(\\.|-|_|((19|20)\\d{2}))", " ").trim();
			log.debug(movieTitle);
			if (StringUtils.isNotBlank(movieTitle)) {
				try {
					ResultList<MovieInfo> resultList = theMovieDbApi.searchMovie(movieTitle, 0, null, true, null, null, SearchType.NGRAM);
					if (resultList.getTotalResults() > 0) {
						log.info("Found " + resultList.getResults().get(0).getOriginalTitle());
					} else {
						log.info("Not found");
					}
				} catch (MovieDbException e) {
					log.error("Error trying to guess movie by clean name", e);
				}
			}
		}
		return null;
	}

	public MovieInfo getMovieInfo(String release) {
		log.info("Trying to guess movie with name and year");
		MovieInfo movieInfo = guessByMovieNameAndYear(release);
		if (movieInfo == null) {
			// log.info("No lucky guess movie with name and year,
			// trying by clean name");
			// movieInfo = guessByCleanName(release);
		}
		return movieInfo;
	}

	public ReleaseType getReleaseType(String release) {

		MovieInfo movieInfo = getMovieInfo(release);
		if (movieInfo != null) {
			return ReleaseType.MOVIE;
		}
		return ReleaseType.OTHER;
	}
}