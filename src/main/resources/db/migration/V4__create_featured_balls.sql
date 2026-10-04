-- Initial selections were randomly drawn from Bowwwl brand listings and verified on 2026-10-04.
CREATE TABLE featured_ball (
    brand_slug VARCHAR(40) PRIMARY KEY CHECK (brand_slug IN ('storm', 'ebonite', '900-global', 'brunswick')),
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(255) NOT NULL,
    source_url VARCHAR(1024) NOT NULL,
    ball_image_url VARCHAR(2048) NOT NULL,
    core_image_url VARCHAR(2048) NOT NULL,
    core_name VARCHAR(255),
    coverstock_type VARCHAR(255),
    core_type VARCHAR(255),
    selected_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO featured_ball (brand_slug, name, brand, source_url, ball_image_url, core_image_url, core_name, coverstock_type, core_type, selected_at)
VALUES ('storm', 'Phaze Crimson', 'Storm', 'https://www.bowwwl.com/bowling-ball-database/storm/phaze-crimson', 'https://www.bowwwl.com/sites/default/files/styles/ball_grid/public/balls/storm-phaze-crimson.png?itok=mNI2S-it', 'https://www.bowwwl.com/sites/default/files/styles/ball_image_main/public/cores/storm-velocity-ai-core.png?itok=YpQv0tgH', 'Velocity A.I. Core', 'Pearl Reactive', 'Symmetric', CURRENT_TIMESTAMP);

INSERT INTO featured_ball (brand_slug, name, brand, source_url, ball_image_url, core_image_url, core_name, coverstock_type, core_type, selected_at)
VALUES ('ebonite', 'Spartan', 'Ebonite', 'https://www.bowwwl.com/bowling-ball-database/ebonite/spartan', 'https://www.bowwwl.com/sites/default/files/styles/ball_grid/public/balls/ebonite-spartan.png?itok=bEfNke_7', 'https://www.bowwwl.com/sites/default/files/styles/ball_image_main/public/cores/ebonite-iron-fist-v2-core.png?itok=oe2f23Az', 'Iron Fist V2 Core', NULL, 'Asymmetric', CURRENT_TIMESTAMP);

INSERT INTO featured_ball (brand_slug, name, brand, source_url, ball_image_url, core_image_url, core_name, coverstock_type, core_type, selected_at)
VALUES ('900-global', 'Reality Incursion', '900 Global', 'https://www.bowwwl.com/bowling-ball-database/900-global/reality-incursion', 'https://www.bowwwl.com/sites/default/files/styles/ball_grid/public/balls/900-global-reality-incursion.png?itok=Oab0meIh', 'https://www.bowwwl.com/sites/default/files/styles/ball_image_main/public/cores/900-global-disturbance-ai-core.png?itok=xIli2so0', 'Disturbance AI Core', 'Solid Reactive', 'Asymmetric', CURRENT_TIMESTAMP);

INSERT INTO featured_ball (brand_slug, name, brand, source_url, ball_image_url, core_image_url, core_name, coverstock_type, core_type, selected_at)
VALUES ('brunswick', 'Fury Orange/Red Pearl', 'Brunswick', 'https://www.bowwwl.com/bowling-ball-database/brunswick/fury-orangered-pearl', 'https://www.bowwwl.com/sites/default/files/styles/ball_grid/public/balls/brunswick-fury-orange-red.png?itok=sQmG3vMG', 'https://www.bowwwl.com/sites/default/files/styles/ball_image_main/public/cores/brunswick-fury-core.png?itok=s0A_VOEv', 'Fury Core', 'Pearl Reactive', 'Symmetric', CURRENT_TIMESTAMP);

