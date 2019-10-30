import PropTypes from 'prop-types';

export const groupShape = {
  icon: PropTypes.string,
  courseName: PropTypes.string,
  courseId: PropTypes.string,
  certificate: PropTypes.shape({
    certificateName: PropTypes.string,
    certificateId: PropTypes.string
  }),
  credential: PropTypes.shape({
    credentialName: PropTypes.string,
    credentialId: PropTypes.string
  }),
  websiteLink: PropTypes.string,
  description: PropTypes.string,
  lastUpdate: PropTypes.number
};
