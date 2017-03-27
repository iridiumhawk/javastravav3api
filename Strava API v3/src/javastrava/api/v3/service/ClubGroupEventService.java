package javastrava.api.v3.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javastrava.api.v3.auth.model.Token;
import javastrava.api.v3.auth.ref.AuthorisationScope;
import javastrava.api.v3.model.StravaAthlete;
import javastrava.api.v3.model.StravaClubEvent;
import javastrava.api.v3.model.StravaClubEventJoinResponse;
import javastrava.util.Paging;

/**
 * <p>
 * {@link StravaClubEvent} related services
 * </p>
 *
 * <p>
 * Group Events are optionally recurring events for club members.
 * </p>
 *
 * <p>
 * Only club members can access private club events.
 * </p>
 *
 * <p>
 * The objects are returned in summary or detailed representations.
 * </p>
 *
 * @author Dan Shannon
 *
 */
public interface ClubGroupEventService extends StravaService {
	/**
	 * <p>
	 * Returns a single group event summary representation.
	 * </p>
	 *
	 * @param id
	 *            The identifier of the group event
	 * @return The group event, or <code>null</code> if the event does not exist, or an event with resource state set to PRIVATE if the club is private
	 */
	public StravaClubEvent getEvent(Integer id);

	/**
	 * <p>
	 * Returns a single group event summary representation.
	 * </p>
	 *
	 * @param id
	 *            The identifier of the group event
	 * @return The response indicating whether the authenticated athlete has joined the event, inside a completable future. Returned event will be <code>null</code> if the event with the given id
	 *         doesn't exist
	 */
	public CompletableFuture<StravaClubEvent> getEventAsync(Integer id);

	/**
	 * <p>
	 * Join a group {@link StravaClubEvent event} on behalf of the authenticated {@link StravaAthlete athlete}. For recurring events, join the upcoming occurrence. An {@link Token access token} with
	 * {@link AuthorisationScope#WRITE write scope} is required.
	 * </p>
	 *
	 * @param id
	 *            The identifier of the group event
	 * @return The response indicating whether the authenticated athlete has joined the event
	 */
	public StravaClubEventJoinResponse joinEvent(Integer id);

	/**
	 * <p>
	 * Join a group {@link StravaClubEvent event} on behalf of the authenticated {@link StravaAthlete athlete}. For recurring events, join the upcoming occurrence. An {@link Token access token} with
	 * {@link AuthorisationScope#WRITE write scope} is required.
	 * </p>
	 *
	 * @param id
	 *            The identifier of the group event
	 * @return The response indicating whether the authenticated athlete has joined the event, inside a completable future
	 */
	public CompletableFuture<StravaClubEventJoinResponse> joinEventAsync(Integer id);

	/**
	 * <p>
	 * Leave a group {@link StravaClubEvent event} on behalf of the authenticated {@link StravaAthlete athlete}. For recurring events, leave the upcoming occurrence. An {@link Token access token} with
	 * {@link AuthorisationScope#WRITE write scope} is required.
	 * </p>
	 *
	 * @param id
	 *            The identifier of the group event
	 * @return The response indicating whether the authenticated athlete has joined the event
	 */
	public StravaClubEventJoinResponse leaveEvent(Integer id);

	/**
	 * <p>
	 * Leave a group {@link StravaClubEvent event} on behalf of the authenticated {@link StravaAthlete athlete}. For recurring events, leave the upcoming occurrence. An {@link Token access token} with
	 * {@link AuthorisationScope#WRITE write scope} is required.
	 * </p>
	 *
	 * @param id
	 *            The identifier of the group event
	 * @return The response indicating whether the authenticated athlete has joined the event, inside a completable future
	 */
	public CompletableFuture<StravaClubEventJoinResponse> leaveEventAsync(Integer id);

	/**
	 * <p>
	 * Retrieve summary information about athletes joined a specific group event, or the upcoming occurrence for recurring events.
	 * </p>
	 *
	 * <p>
	 * Pagination is supported.
	 * </p>
	 *
	 * <p>
	 * Returns an array of athletes summary representations with athletes who the authenticated athlete is following first.
	 * </p>
	 *
	 * <p>
	 * Returns <code>null</code> if the event does not exist
	 * </p>
	 *
	 * <p>
	 * Returns an empty list if the event is private or belongs to a club that is private that the authenticated athlete is not a member of
	 * </p>
	 *
	 * @param eventId
	 *            The identifier of the event for which athletes should be listed
	 * @param pagingInstruction
	 *            Paging instruction
	 * @return Array of athletes who have joined the event
	 */
	public List<StravaAthlete> listEventJoinedAthletes(Integer eventId, Paging pagingInstruction);

	/**
	 * <p>
	 * Retrieve summary information about ALL athletes joined a specific group event, or the upcoming occurrence for recurring events, regardless of how many there are.
	 * </p>
	 *
	 * <p>
	 * Pagination is NOT supported.
	 * </p>
	 *
	 * <p>
	 * Returns an array of athletes summary representations with athletes who the authenticated athlete is following first.
	 * </p>
	 *
	 * <p>
	 * Returns <code>null</code> if the event does not exist
	 * </p>
	 *
	 * <p>
	 * Returns an empty list if the event is private or belongs to a club that is private that the authenticated athlete is not a member of
	 * </p>
	 *
	 * @param eventId
	 *            The identifier of the event for which athletes should be listed
	 * @return Array of athletes who have joined the event
	 */
	public List<StravaAthlete> listAllEventJoinedAthletes(Integer eventId);

	/**
	 * <p>
	 * Retrieve summary information about athletes joined a specific group event, or the upcoming occurrence for recurring events.
	 * </p>
	 * <p>
	 * Pagination is supported.
	 * </p>
	 * <p>
	 * Returns a completable future which can be used later to return an array of athletes summary representations with athletes who the authenticated athlete is following first.
	 * </p>
	 *
	 * @param eventId
	 *            The identifier of the event for which athletes should be listed
	 * @param pagingInstruction
	 *            Paging instruction
	 * @return Array of athletes who have joined the event
	 */
	public CompletableFuture<List<StravaAthlete>> listEventJoinedAthletesAsync(Integer eventId, Paging pagingInstruction);

	/**
	 * <p>
	 * Retrieve summary information about ALL athletes joined a specific group event, or the upcoming occurrence for recurring events, regardless of how many there are.
	 * </p>
	 *
	 * <p>
	 * Pagination is NOT supported.
	 * </p>
	 *
	 * <p>
	 * Returns a completable future which can later be used to return an array of athletes summary representations with athletes who the authenticated athlete is following first.
	 * </p>
	 *
	 * <p>
	 * Returns <code>null</code> if the event does not exist
	 * </p>
	 *
	 * <p>
	 * Returns an empty list if the event is private or belongs to a club that is private that the authenticated athlete is not a member of
	 * </p>
	 *
	 * @param eventId
	 *            The identifier of the event for which athletes should be listed
	 * @return Array of athletes who have joined the event
	 */
	public CompletableFuture<List<StravaAthlete>> listAllEventJoinedAthletesAsync(Integer eventId);

}
