/**
 * 
 */
package javastrava.api.v3.service.impl.retrofit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javastrava.api.v3.auth.model.Token;
import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.model.StravaAthlete;
import javastrava.api.v3.model.StravaClub;
import javastrava.api.v3.model.StravaClubMembershipResponse;
import javastrava.api.v3.model.reference.StravaResourceState;
import javastrava.api.v3.service.ClubServices;
import javastrava.api.v3.service.PagingCallback;
import javastrava.api.v3.service.PagingHandler;
import javastrava.api.v3.service.exception.NotFoundException;
import javastrava.api.v3.service.exception.UnauthorizedException;
import javastrava.util.Paging;

/**
 * <p>
 * Implementation of {@link ClubServices}
 * </p>
 * 
 * @author Dan Shannon
 *
 */
public class ClubServicesImpl extends StravaServiceImpl<ClubServicesRetrofit> implements ClubServices {
	private ClubServicesImpl(final Token token) {
		super(ClubServicesRetrofit.class,token);
	}

	/**
	 * <p>
	 * Returns an implementation of {@link ClubServices club services}
	 * </p>
	 * 
	 * <p>
	 * Instances are cached so that if 2 requests are made for the same token, the same instance is returned
	 * </p>
	 * 
	 * @param token
	 *            The Strava access token to be used in requests to the Strava API
	 * @return An implementation of the club services
	 * @throws UnauthorizedException
	 *             If the token used to create the service is invalid
	 */
	public static ClubServices implementation(final Token token) {
		// Get the service from the token's cache
		ClubServices service = token.getService(ClubServices.class);
		
		// If it's not already there, create a new one and put it in the token
		if (service == null) {
			service = new ClubServicesImpl(token);
			token.addService(ClubServices.class, service);
		}
		return service;
	}

	/**
	 * @see javastrava.api.v3.service.ClubServices#getClub(java.lang.Integer)
	 */
	@Override
	public StravaClub getClub(final Integer id) {
		try {
			return this.restService.getClub(id);
		} catch (NotFoundException e) {
			return null;
		} catch (UnauthorizedException e) {
			if (accessTokenIsValid()) {
				// If we get here, the access token is valid
				// Therefore the club is private, so return an empty club
				return privateClubRepresentation(id);
			} else {
				throw e;
			}
		}
	}

	private static StravaClub privateClubRepresentation(final Integer id) {
		StravaClub club = new StravaClub();
		club.setId(id);
		club.setResourceState(StravaResourceState.META);
		return club;
	}

	/**
	 * @see javastrava.api.v3.service.ClubServices#listAuthenticatedAthleteClubs()
	 */
	@Override
	public List<StravaClub> listAuthenticatedAthleteClubs() {
		return Arrays.asList(this.restService.listAuthenticatedAthleteClubs());
	}

	/**
	 * @see javastrava.api.v3.service.ClubServices#listClubMembers(Integer, Paging)
	 */
	@Override
	public List<StravaAthlete> listClubMembers(final Integer id, final Paging pagingInstruction) {
		return PagingHandler.handlePaging(pagingInstruction, new PagingCallback<StravaAthlete>() {
			@Override
			public List<StravaAthlete> getPageOfData(final Paging thisPage) throws NotFoundException {
				return Arrays.asList(ClubServicesImpl.this.restService.listClubMembers(id, thisPage.getPage(), thisPage.getPageSize()));
			}
		});
	}

	/**
	 * @see javastrava.api.v3.service.ClubServices#listRecentClubActivities(Integer, Paging)
	 */
	@Override
	public List<StravaActivity> listRecentClubActivities(final Integer id, final Paging pagingInstruction) {
		return PagingHandler.handlePaging(pagingInstruction, new PagingCallback<StravaActivity>() {
			@Override
			public List<StravaActivity> getPageOfData(final Paging thisPage) throws NotFoundException {
				return Arrays.asList(ClubServicesImpl.this.restService.listRecentClubActivities(id, thisPage.getPage(), thisPage.getPageSize()));
			}
		});
	}

	/**
	 * @see javastrava.api.v3.service.ClubServices#joinClub(java.lang.Integer)
	 */
	@Override
	public StravaClubMembershipResponse joinClub(final Integer id) {
		try {
			return this.restService.join(id);
		} catch (NotFoundException e) {
			return failedClubMembershipResponse();
		} catch (UnauthorizedException e) {
			if (accessTokenIsValid()) {
				return failedClubMembershipResponse();
			} else {
				throw e;
			}
		}
	}

	private static StravaClubMembershipResponse failedClubMembershipResponse() {
		StravaClubMembershipResponse response = new StravaClubMembershipResponse();
		response.setActive(Boolean.FALSE);
		response.setSuccess(Boolean.FALSE);
		return response;
	}

	/**
	 * @see javastrava.api.v3.service.ClubServices#leaveClub(java.lang.Integer)
	 */
	@Override
	public StravaClubMembershipResponse leaveClub(final Integer id) {
		try {
			return this.restService.leave(id);
		} catch (UnauthorizedException e) {
			if (accessTokenIsValid()) {
				return failedClubMembershipResponse();
			} else {
				throw e;
			}
		} catch (NotFoundException e) {
			return failedClubMembershipResponse();
		}
	}

	/**
	 * @see javastrava.api.v3.service.ClubServices#listClubMembers(java.lang.Integer)
	 */
	@Override
	public List<StravaAthlete> listClubMembers(final Integer id) {
		return listClubMembers(id, null);
	}

	/**
	 * @see javastrava.api.v3.service.ClubServices#listRecentClubActivities(java.lang.Integer)
	 */
	@Override
	public List<StravaActivity> listRecentClubActivities(final Integer id) {
		List<StravaActivity> activities = listRecentClubActivities(id, null);

		// Strava API returns NULL instead of an empty array
		if (activities == null) {
			activities = new ArrayList<StravaActivity>();
		}
		return activities;
	}

	/**
	 * @see javastrava.api.v3.service.ClubServices#listAllClubMembers(java.lang.Integer)
	 */
	@Override
	public List<StravaAthlete> listAllClubMembers(final Integer clubId) {
		return PagingHandler.handleListAll(new PagingCallback<StravaAthlete>() {

			@Override
			public List<StravaAthlete> getPageOfData(final Paging thisPage) throws NotFoundException {
				return listClubMembers(clubId,thisPage);
			}
			
		});
		
	}

	/**
	 * @see javastrava.api.v3.service.ClubServices#listAllRecentClubActivities(java.lang.Integer)
	 */
	@Override
	public List<StravaActivity> listAllRecentClubActivities(final Integer clubId) {
		return PagingHandler.handleListAll(new PagingCallback<StravaActivity>() {

			@Override
			public List<StravaActivity> getPageOfData(final Paging thisPage) throws NotFoundException {
				return listRecentClubActivities(clubId, thisPage);
			}
			
		});
	}

}
