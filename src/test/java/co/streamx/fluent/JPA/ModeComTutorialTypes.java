package co.streamx.fluent.JPA;

import java.sql.Timestamp;

import jakarta.persistence.Table;

import co.streamx.fluent.notation.Tuple;
import lombok.Data;

public interface ModeComTutorialTypes {

    @Tuple()
    @Data
    @Table(name = "crunchbase_companies_clean_date", schema = "tutorial")
    class CrunchbaseCompany {
        private String permalink;
        private String foundedAtClean;
    }

    @Tuple()
    @Data
    @Table(name = "crunchbase_acquisitions_clean_date", schema = "tutorial")
    class CrunchbaseAcquisition {
        private String companyPermalink;
        private Timestamp acquiredAtCleaned;
    }

    @Tuple()
    @Data
    @Table(name = "sf_crime_incidents_2014_01", schema = "tutorial")
    class CrimeIncidents2014_01 {
        private String location;
        private String descript;
        private int incidntNum;
        private String date;
        private String dayOfWeek;
        private String resolution;
    }

    @Tuple()
    @Data
    @Table(name = "dc_bikeshare_q1_2012", schema = "tutorial")
    class BikeShareQ12012 {
        private Timestamp startTime;
        private int durationSeconds;
        private int startTerminal;
    }
}
