package com.github.hhhzzzsss.songplayer.song;

public class Note implements Comparable<Note> {
	public int noteId;
	public long time;
	public byte volume;
	public Note(int note, long time, byte volume) {
		this.noteId = note;
		this.time = time;
		this.volume = volume;
	}

	@Override
	public int compareTo(Note other) {
		if (time < other.time) {
			return -1;
		}
		else if (time > other.time) {
			return 1;
		}
		else {
			return 0;
		}
	}
}
