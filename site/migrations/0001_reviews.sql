-- Reviews sent from the site. Nothing is shown until the owner sets status to 'approved'.
CREATE TABLE reviews (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT,
  rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
  devices TEXT NOT NULL,
  body TEXT NOT NULL,
  lang TEXT NOT NULL,
  can_publish INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
  created_at TEXT NOT NULL
);
CREATE INDEX reviews_status_created ON reviews (status, created_at DESC);
