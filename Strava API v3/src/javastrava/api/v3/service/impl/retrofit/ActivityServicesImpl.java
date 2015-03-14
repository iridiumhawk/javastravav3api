package javastrava.api.v3.service.impl.retrofit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javastrava.api.v3.auth.model.Token;
import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.model.StravaActivityUpdate;
import javastrava.api.v3.model.StravaActivityZone;
import javastrava.api.v3.model.StravaAthlete;
import javastrava.api.v3.model.StravaComment;
import javastrava.api.v3.model.StravaLap;
import javastrava.api.v3.model.StravaPhoto;
import javastrava.api.v3.model.StravaSegmentEffort;
import javastrava.api.v3.model.reference.StravaResourceState;
import javastrava.api.v3.service.ActivityServices;
import javastrava.api.v3.service.PagingCallback;
import javastrava.api.v3.service.PagingHandler;
import javastrava.api.v3.service.exception.BadRequestException;
import javastrava.api.v3.service.exception.NotFoundException;
import javastrava.api.v3.service.exception.StravaUnknownAPIException;
import javastrava.api.v3.service.exception.UnauthorizedException;
import javastrava.config.Messages;
import javastrava.util.Paging;

/**
 * @author Dan Shannon
 *
 */
public class ActivityServicesImpl extends StravaServiceImpl<ActivityServicesRetrofit> implements ActivityServices {
	private ActivityServicesImpl(final Token token) {
		super(ActivityServicesRetrofit.class,token);
	}

	/**
	 * <p>
	 * Returns an implementation of {@link ActivityServices activity services}
	 * </p>
	 * 
	 * <p>
	 * Instances are cached so that if 2 requests are made for the same token, the same instance is returned
	 * </p>
	 * 
	 * @param token
	 *            The Strava access token to be used in requests to the Strava API
	 * @return An implementation of the activity services
	 */
	public static ActivityServices implementation(final Token token) {
		// Get the service from the token's cache
		ActivityServices service = token.getService(ActivityServices.class);
		
		// If it's not already there, create a new one and put it in the token
		if (service == null) {
			service = new ActivityServicesImpl(token);
			token.addService(ActivityServices.class, service);
		}
		return service;
	}		
		
	/**
	 * @see javastrava.api.v3.service.ActivityServices#getActivity(java.lang.Integer, java.lang.Boolean)
	 */
	@Override
	public StravaActivity getActivity(final Integer id, final Boolean includeAllEfforts) {
		StravaActivity stravaResponse = null;
		
		try {
			boolean loop = true;
			int i = 0;
			while (loop) {
				i++;
				stravaResponse = this.restService.getActivity(id, includeAllEfforts);
		
				// If the activity is being updated, wait for the update to complete
				if (i < 10 && stravaResponse.getResourceState() == StravaResourceState.UPDATING) {
					try {
						Thread.sleep(1000 + i * 100);
					} catch (InterruptedException e) {
						// Ignore
					}
				} else {
					loop = false;
				}
			}
			
			if (stravaResponse.getSegmentEfforts() != null) {
				for (StravaSegmentEffort effort : stravaResponse.getSegmentEfforts()) {
					// TODO This is a workaround for a Strava bug (Issue javastrava-api #11)
					if (effort.getActivity().getResourceState() == null) {
						effort.getActivity().setResourceState(StravaResourceState.META);
					}
					// TODO This is a workaround for a Strava bug (Issue javastrava-api #12)
					if (effort.getAthlete().getResourceState() == null) {
						effort.getAthlete().setResourceState(StravaResourceState.META);
					}
				}
			}
			
			return stravaResponse;

		} catch (NotFoundException e) {
			// Activity doesn't exist - return null
			return null;
		} catch (UnauthorizedException e) {
			if (accessTokenIsValid()) {
				// Activity is private
				StravaActivity activity = new StravaActivity();
				activity.setId(id);
				activity.setResourceState(StravaResourceState.META);
				return activity;
			} else {
				throw e;
			}
		}
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#createManualActivity(javastrava.api.v3.model.StravaActivity)
	 */
	@Override
	public StravaActivity createManualActivity(final StravaActivity activity) {
		try {
			return this.restService.createManualActivity(activity);
		} catch (BadRequestException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#updateActivity(Integer,javastrava.api.v3.model.StravaActivityUpdate)
	 */
	@Override
	public StravaActivity updateActivity(final Integer id, final StravaActivityUpdate activity) {
		StravaActivityUpdate update = activity;
		if (activity == null) {
			return getActivity(id);
		}
		StravaActivity response = null;
		
		
		// TODO Workaround for issue javastrava-api #36 (https://github.com/danshannon/javastravav3api/issues/36)
		if (update.getCommute() != null) {
			StravaActivityUpdate commuteUpdate = new StravaActivityUpdate();
			commuteUpdate.setCommute(update.getCommute());
			response = doUpdateActivity(id, commuteUpdate);
			if (response.getCommute() != update.getCommute()) { 
				throw new StravaUnknownAPIException(Messages.getString("ActivityServicesImpl.failedToUpdateCommuteFlag") + id, null, null); //$NON-NLS-1$
			}
			
			update.setCommute(null);
		}

		// End of workaround
		
		response = doUpdateActivity(id, update);
		return response;
		
	}		
	private StravaActivity doUpdateActivity(final Integer id, final StravaActivityUpdate update) {
		try {
			StravaActivity response = this.restService.updateActivity(id, update);
			if (response.getResourceState() == StravaResourceState.UPDATING) {
				response = getActivity(id);
			}
			return response;
		} catch (NotFoundException e) {
			return null;
		}
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#deleteActivity(java.lang.Integer)
	 */
	@Override
	public StravaActivity deleteActivity(final Integer id) {
		try {
			return this.restService.deleteActivity(id);
		} catch (NotFoundException e) {
			return null;
		}
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listAuthenticatedAthleteActivities(Calendar, Calendar, Paging)
	 */
	@Override
	public List<StravaActivity> listAuthenticatedAthleteActivities(final Calendar before, final Calendar after, final Paging pagingInstruction) {
		final Integer secondsBefore = secondsSinceUnixEpoch(before);
		final Integer secondsAfter = secondsSinceUnixEpoch(after);

		return PagingHandler.handlePaging(pagingInstruction, new PagingCallback<StravaActivity>() {
			@Override
			public List<StravaActivity> getPageOfData(final Paging thisPage) throws NotFoundException {
				return Arrays.asList(ActivityServicesImpl.this.restService.listAuthenticatedAthleteActivities(secondsBefore, secondsAfter, thisPage.getPage(),
						thisPage.getPageSize()));
			}
		});
	}

	/**
	 * @param date Date for which seconds since the epoch date is to be calculated
	 * @return Number of seconds after the unix epoch date equivalent to the given date
	 */
	private static Integer secondsSinceUnixEpoch(final Calendar date) {
		if (date == null) {
			return null;
		}
		Long timeInSeconds = Long.valueOf(date.getTimeInMillis() / 1000L);
		return Integer.valueOf(timeInSeconds.intValue());
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listFriendsActivities(Paging)
	 */
	@Override
	public List<StravaActivity> listFriendsActivities(final Paging pagingInstruction) {
		List<StravaActivity> activities = PagingHandler.handlePaging(pagingInstruction, new PagingCallback<StravaActivity>() {
			@Override
			public List<StravaActivity> getPageOfData(final Paging thisPage) throws NotFoundException {
				return Arrays.asList(ActivityServicesImpl.this.restService.listFriendsActivities(thisPage.getPage(), thisPage.getPageSize()));
			}
		});
		return activities;
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listActivityZones(java.lang.Integer)
	 */
	@Override
	public List<StravaActivityZone> listActivityZones(final Integer id) {
		try {
			return Arrays.asList(this.restService.listActivityZones(id));
		} catch (NotFoundException e) {
			return null;
		} catch (UnauthorizedException e) {
			if (accessTokenIsValid()) {
				return new ArrayList<StravaActivityZone>();
			} else {
				throw e;
			}
		}
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listActivityLaps(java.lang.Integer)
	 */
	@Override
	public List<StravaLap> listActivityLaps(final Integer id) {
		try {
			List<StravaLap> laps = Arrays.asList(this.restService.listActivityLaps(id));
			
			for (StravaLap lap : laps) {
				// TODO This is a workaround for Strava issue javastrava-api #15
				if (lap.getActivity().getResourceState() == null) {
					lap.getActivity().setResourceState(StravaResourceState.META);
				}
				// TODO This is a workaround for Strava issue javastrava-api #16
				if (lap.getAthlete().getResourceState() == null) {
					lap.getAthlete().setResourceState(StravaResourceState.META);
				}
				// TODO This is a workaround for Strava issue javastrava-api #17
				if (lap.getAverageWatts() != null && lap.getDeviceWatts() == null) {
					lap.setDeviceWatts(Boolean.FALSE);
				}
			}
			return laps;
		} catch (NotFoundException e) {
			return null;
		} catch (UnauthorizedException e) {
			if (accessTokenIsValid()) {
				return new ArrayList<StravaLap>();
			} else {
				throw e;
			}
		}
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listActivityComments(Integer, Boolean, Paging)
	 */
	@Override
	public List<StravaComment> listActivityComments(final Integer id, final Boolean markdown, final Paging pagingInstruction) {
		return PagingHandler.handlePaging(pagingInstruction, new PagingCallback<StravaComment>() {
			@Override
			public List<StravaComment> getPageOfData(final Paging thisPage) throws NotFoundException {
				return Arrays.asList(ActivityServicesImpl.this.restService.listActivityComments(id, markdown, thisPage.getPage(), thisPage.getPageSize()));
			}
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listActivityKudoers(Integer, Paging)
	 */
	@Override
	public List<StravaAthlete> listActivityKudoers(final Integer id, final Paging pagingInstruction) {
		return PagingHandler.handlePaging(pagingInstruction, new PagingCallback<StravaAthlete>() {
			@Override
			public List<StravaAthlete> getPageOfData(final Paging thisPage) throws NotFoundException {
				return Arrays.asList(ActivityServicesImpl.this.restService.listActivityKudoers(id, thisPage.getPage(), thisPage.getPageSize()));
			}
		});

	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listActivityPhotos(java.lang.Integer)
	 */
	@Override
	public List<StravaPhoto> listActivityPhotos(final Integer id) {
		try {
			StravaPhoto[] photos = this.restService.listActivityPhotos(id);

			// This fixes an inconsistency with the listActivityComments API
			// call on Strava, which returns an empty array, not null
			if (photos == null) {
				photos = new StravaPhoto[0];
			}

			return Arrays.asList(photos);

		} catch (NotFoundException e) {
			return null;
		} catch (UnauthorizedException e) {
			if (accessTokenIsValid()) {
				return new ArrayList<StravaPhoto>();
			} else {
				throw e;
			}
		}
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listActivityComments(java.lang.Integer, java.lang.Boolean)
	 */
	@Override
	public List<StravaComment> listActivityComments(final Integer id, final Boolean markdown) {
		return listActivityComments(id, markdown, null);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listActivityKudoers(java.lang.Integer)
	 */
	@Override
	public List<StravaAthlete> listActivityKudoers(final Integer id) {
		return listActivityKudoers(id, null);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listAuthenticatedAthleteActivities(java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public List<StravaActivity> listAuthenticatedAthleteActivities(final Calendar before, final Calendar after) {
		return listAuthenticatedAthleteActivities(before, after, null);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listFriendsActivities()
	 */
	@Override
	public List<StravaActivity> listFriendsActivities() {
		return listFriendsActivities(null);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listAuthenticatedAthleteActivities()
	 */
	@Override
	public List<StravaActivity> listAuthenticatedAthleteActivities() {
		return listAuthenticatedAthleteActivities(null, null, null);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listAuthenticatedAthleteActivities(javastrava.util.Paging)
	 */
	@Override
	public List<StravaActivity> listAuthenticatedAthleteActivities(final Paging pagingInstruction) {
		return listAuthenticatedAthleteActivities(null, null, pagingInstruction);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listRelatedActivities(java.lang.Integer)
	 */
	@Override
	public List<StravaActivity> listRelatedActivities(final Integer id) {
		return listRelatedActivities(id, null);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listRelatedActivities(java.lang.Integer, javastrava.util.Paging)
	 */
	@Override
	public List<StravaActivity> listRelatedActivities(final Integer id, final Paging pagingInstruction) {
		return PagingHandler.handlePaging(pagingInstruction, new PagingCallback<StravaActivity>() {
			@Override
			public List<StravaActivity> getPageOfData(final Paging thisPage) throws NotFoundException {
				return Arrays.asList(ActivityServicesImpl.this.restService.listRelatedActivities(id, thisPage.getPage(), thisPage.getPageSize()));
			}
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#getActivity(java.lang.Integer)
	 */
	@Override
	public StravaActivity getActivity(final Integer id) {
		return getActivity(id, Boolean.FALSE);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listActivityComments(java.lang.Integer)
	 */
	@Override
	public List<StravaComment> listActivityComments(final Integer id) {
		return listActivityComments(id, Boolean.FALSE);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listActivityComments(java.lang.Integer, javastrava.util.Paging)
	 */
	@Override
	public List<StravaComment> listActivityComments(final Integer id, final Paging pagingInstruction) {
		return listActivityComments(id, Boolean.FALSE, pagingInstruction);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listAllAuthenticatedAthleteActivities()
	 */
	@Override
	public List<StravaActivity> listAllAuthenticatedAthleteActivities() {
		return PagingHandler.handleListAll(new PagingCallback<StravaActivity>() {

			@Override
			public List<StravaActivity> getPageOfData(final Paging thisPage) throws NotFoundException {
				return listAuthenticatedAthleteActivities(thisPage);
			}
			
		});
		
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#createComment(java.lang.Integer, java.lang.String)
	 */
	@Override
	public StravaComment createComment(final Integer id, final String text) throws NotFoundException, BadRequestException {
		if (text == null || text.equals("")) { //$NON-NLS-1$
			throw new IllegalArgumentException(Messages.getString("ActivityServicesImpl.commentCannotBeEmpty")); //$NON-NLS-1$
		}
		// TODO Workaround for issue javastrava-api #30 (https://github.com/danshannon/javastravav3api/issues/30)
		if (!(getToken().hasWriteAccess())) {
			throw new UnauthorizedException(Messages.getString("ActivityServicesImpl.commentWithoutWriteAccess")); //$NON-NLS-1$
		}
		// End of workaround
		return this.restService.createComment(id, text);
				
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#deleteComment(java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public void deleteComment(final Integer activityId, final Integer commentId) throws NotFoundException {
		this.restService.deleteComment(activityId, commentId);
		
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#deleteComment(javastrava.api.v3.model.StravaComment)
	 */
	@Override
	public void deleteComment(final StravaComment comment) throws NotFoundException {
		
		this.restService.deleteComment(comment.getActivityId(), comment.getId());
		
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#giveKudos(java.lang.Integer)
	 */
	@Override
	public void giveKudos(final Integer activityId) throws NotFoundException {
		// TODO Workaround for issue javastrava-api #29 (https://github.com/danshannon/javastravav3api/issues/29)
		if (!(getToken().hasWriteAccess())) {
			throw new UnauthorizedException(Messages.getString("ActivityServicesImpl.kudosWithoutWriteAccess")); //$NON-NLS-1$
		}
		// End of workaround
		
		this.restService.giveKudos(activityId);
		
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listAllActivityComments(java.lang.Integer)
	 */
	@Override
	public List<StravaComment> listAllActivityComments(final Integer activityId) {
		return PagingHandler.handleListAll(new PagingCallback<StravaComment>() {

			@Override
			public List<StravaComment> getPageOfData(final Paging thisPage) throws NotFoundException {
				return listActivityComments(activityId, thisPage);
			}
			
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listAllActivityKudoers(java.lang.Integer)
	 */
	@Override
	public List<StravaAthlete> listAllActivityKudoers(final Integer activityId) {
		return PagingHandler.handleListAll(new PagingCallback<StravaAthlete>() {

			@Override
			public List<StravaAthlete> getPageOfData(final Paging thisPage) throws NotFoundException {
				return listActivityKudoers(activityId, thisPage);
			}
			
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listAllRelatedActivities(java.lang.Integer)
	 */
	@Override
	public List<StravaActivity> listAllRelatedActivities(final Integer activityId) {
		return PagingHandler.handleListAll(new PagingCallback<StravaActivity>() {

			@Override
			public List<StravaActivity> getPageOfData(final Paging thisPage) throws NotFoundException {
				return listRelatedActivities(activityId, thisPage);
			}
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listAllAuthenticatedAthleteActivities(java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public List<StravaActivity> listAllAuthenticatedAthleteActivities(final Calendar before, final Calendar after) {
		return PagingHandler.handleListAll(new PagingCallback<StravaActivity>() {

			@Override
			public List<StravaActivity> getPageOfData(final Paging thisPage) throws NotFoundException {
				return listAuthenticatedAthleteActivities(before, after, thisPage);
			}
			
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityServices#listAllFriendsActivities()
	 */
	@Override
	public List<StravaActivity> listAllFriendsActivities() {
		return PagingHandler.handleListAll(new PagingCallback<StravaActivity>() {

			@Override
			public List<StravaActivity> getPageOfData(final Paging thisPage) throws NotFoundException {
				return listFriendsActivities(thisPage);
			}
			
		});
	}

}
