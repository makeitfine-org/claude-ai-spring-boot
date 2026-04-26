-- Resync sequence to current max ID (guards against manual inserts that bypassed the sequence)
SELECT setval('persons_id_seq', COALESCE((SELECT MAX(id) FROM persons), 1));

-- Change increment to match Hibernate allocationSize=20 (pooled optimizer)
ALTER SEQUENCE persons_id_seq INCREMENT BY 20;
