package com.theFox6.kleinerNerd.categories;

public enum ConfigState {
	UNCONFIGURED,
	EDIT_ROLE, //whether to edit the configured role or not 
	SET_ROLE, //set the role that has access
	//TODO create/configure text and voice channels
	CONFIGURE_ANNCOUNCE, //configure whether to announce the category and whether to add a rr
	CHOOSE_ANNCOUNCE_CHANNEL, //set name or id of the channel the announcement should be in
	ADD_OR_EDIT_ANNCOUNCE, //whether to edit an existing annoucement or create a new one
	ADD_ANNOUNCE, //create an announcement message
	CHOOSE_ANNOUNCE, //choose an existing announcement message
	//EDIT_ANNOUNCE, //edit an existing announcement message
	SET_RR_EMOTE, //set the emote which gives the role
	CONFIGURED, //done
	PART_FAILED, // some kind of ouchy appeared along the way, let's cancel configuring for now
}