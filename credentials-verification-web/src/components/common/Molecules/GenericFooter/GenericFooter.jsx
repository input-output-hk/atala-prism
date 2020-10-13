import React from 'react';
import PropTypes from 'prop-types';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import './_style.scss';

const GenericFooter = ({ previous, next, disablePrevious, disableNext, labels }) => (
  <div className="GenericFooter">
    <div className="LeftButtons">
      {previous && (
        <CustomButton
          buttonProps={{
            onClick: previous,
            className: 'theme-grey',
            disabled: disablePrevious
          }}
          buttonText={labels.previous}
        />
      )}
    </div>
    <div className="RightButtons">
      {next && (
        <CustomButton
          buttonProps={{
            onClick: next,
            className: 'theme-primary',
            disabled: disableNext
          }}
          buttonText={labels.next}
        />
      )}
    </div>
  </div>
);

GenericFooter.defaultProps = {
  previous: undefined,
  next: undefined,
  disablePrevious: false,
  disableNext: false,
  labels: { previous: 'Back', next: 'Next' }
};

GenericFooter.propTypes = {
  previous: PropTypes.func,
  next: PropTypes.func,
  disablePrevious: PropTypes.bool,
  disableNext: PropTypes.bool,
  labels: PropTypes.shape({ previous: PropTypes.string, next: PropTypes.string })
};

export default GenericFooter;
