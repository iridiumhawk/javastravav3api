package javastrava.api.v3.model.reference;

import javastrava.config.Messages;
import javastrava.util.impl.gson.serializer.ResourceStateSerializer;

/**
 * <p>
 * State of a resource returned from Strava.
 * </p>
 * 
 * @author Dan Shannon
 *
 */
public enum StravaResourceState {
	/**
	 * Resource is currently being updated
	 */
	UPDATING(Integer.valueOf(-1), Messages.getString("StravaResourceState.updating.description")),  //$NON-NLS-1$
	/**
	 * This is a representation of the resource which contains the id ONLY (other than the resource state)
	 */
	META(Integer.valueOf(1), Messages.getString("StravaResourceState.meta.description")),  //$NON-NLS-1$
	/**
	 * This is a summary representation of the resource
	 */
	SUMMARY(Integer.valueOf(2), Messages.getString("StravaResourceState.summary.description")),  //$NON-NLS-1$
	/**
	 * This is a detailed representation of the resource
	 */
	DETAILED(Integer.valueOf(3), Messages.getString("StravaResourceState.detailed.description")),  //$NON-NLS-1$
	/**
	 * <p>
	 * Should never occur but may if Strava API behaviour has changed
	 * </p>
	 */
	UNKNOWN(Integer.valueOf(-2), Messages.getString("Common.unknown.description")); //$NON-NLS-1$

	private Integer	id;
	private String	description;

	/**
	 * Used by JSON serialisation
	 * @return The integer representation of this {@link StravaResourceState} to be used with the Strava API
	 * @see ResourceStateSerializer#serialize(StravaResourceState, java.lang.reflect.Type, com.google.gson.JsonSerializationContext)
	 */
	public Integer getValue() {
		return this.id;
	}

	private StravaResourceState(final Integer id, final String description) {
		this.id = id;
		this.description = description;
	}

	/**
	 * Used by JSON deserialisation
	 * @param id The integer representation of this {@link StravaResourceState} as returned by the Strava API
	 * @return The matching {@link StravaResourceState}, or {@link StravaResourceState#UNKNOWN} if there is no match
	 */
	public static StravaResourceState create(final Integer id) {
		StravaResourceState[] states = StravaResourceState.values();
		for (StravaResourceState state : states) {
			if (state.getValue() != null && state.getValue().equals(id)) {
				return state;
			}
		}
		return StravaResourceState.UNKNOWN;
	}

	/**
	 * @return the id
	 */
	public Integer getId() {
		return this.id;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return this.id.toString();
	}
}
