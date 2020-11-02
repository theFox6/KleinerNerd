package com.theFox6.kleinerNerd.data;

import net.dv8tion.jda.api.entities.ISnowflake;

public class SnowflakePair {
	protected ISnowflake flake1;
	protected ISnowflake flake2;
	
	public SnowflakePair(ISnowflake f1, ISnowflake f2) {
		flake1 = f1;
		flake2 = f2;
	}
	
	/**
	 * Checks if the flake Id long values of the other Snowflake pair are the same.
	 */
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof SnowflakePair))
			return false;
		SnowflakePair p = (SnowflakePair) o;
		if (p.flake1.getIdLong() != flake1.getIdLong())
			return false;
		if (p.flake2.getIdLong() != flake2.getIdLong())
			return false;
		return true;
	}

	/**
	 * Calculate a hash code for the snowflake pair.
	 * It is made by applying an exclusive or on their id long values and then hashing that.
	 */
	public int hashCode() {
		return Long.hashCode(flake1.getIdLong()^flake2.getIdLong());
	}
}
