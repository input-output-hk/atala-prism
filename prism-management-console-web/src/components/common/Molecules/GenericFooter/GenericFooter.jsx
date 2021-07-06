import React from 'react';
import PropTypes from 'prop-types';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import './_style.scss';

const GenericFooter = ({ previous, next, disablePrevious, disableNext, loading }) => (
  <div className="GenericFooter">
    <div className="LeftButtons">
      {previous && (
        <CustomButton
          buttonProps={{
            onClick: previous,
            className: 'theme-grey',
            disabled: disablePrevious
          }}
          buttonText={<LeftOutlined />}
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
          buttonText={<RightOutlined />}
          loading={loading}
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
  loading: false,
  labels: { previous: 'Back', next: 'Next' }
};

GenericFooter.propTypes = {
  previous: PropTypes.func,
  next: PropTypes.func,
  disablePrevious: PropTypes.bool,
  disableNext: PropTypes.bool,
  loading: PropTypes.bool,
  labels: PropTypes.shape({ previous: PropTypes.string, next: PropTypes.string })
};

export default GenericFooter;
