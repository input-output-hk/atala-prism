import React, { Component, createRef } from 'react';
import PropTypes from 'prop-types';
import { AtalaPrismDemo } from 'atala-prism-demo';
import atalaLogo from '../../../../images/atala-prism-logo-suite.svg';
import atalaLogo360 from '../../../../images/360-digital-credential-demo.svg';
import cardanoLogo from '../../../../images/powered-by-cardano.svg';
import Logger from '../../../../helpers/Logger';

import './_style.scss';

const FirstStep = 1;

class InteractiveMap extends Component {
  constructor(props) {
    super(props);
    this.setMapStep = this.setMapStep.bind(this);
    this.demo = createRef();
  }

  componentDidMount() {
    const { mapStep } = this.props;
    if (mapStep !== FirstStep) this.setMapStep();
  }

  componentDidUpdate() {
    const { mapStep, mapInitFirstStep, mapReset } = this.props;
    if (mapReset) {
      this.resetMap();
    }
    if (mapInitFirstStep || mapStep !== FirstStep) this.setMapStep();
  }

  setMapStep() {
    const { mapStep } = this.props;
    if (mapStep && this.demo.current) this.demo.current.setStep(mapStep);
  }

  resetMap() {
    const { onReset } = this.props;
    try {
      if (this.demo.current) this.demo.current.reset();
    } catch (error) {
      Logger.error('There has been an error', error);
    }
    if (onReset) onReset();
  }

  render() {
    const { controlsEnabled } = this.props;

    return (
      <div className="MapContainer">
        <div className="LogoContainer">
          <img src={atalaLogo} alt="Atala" className="AtalaLogo" />
          <img
            src={atalaLogo360}
            alt="Atala 360 Digital Credentials Demo"
            className="AtalaLogoDemo"
          />
        </div>
        <div className="AtalaPrismDemoMap">
          <AtalaPrismDemo
            ref={this.demo}
            style={{ outline: 'none' }}
            controlsEnabled={controlsEnabled}
          />
        </div>
        <img src={cardanoLogo} alt="Cardano" className="CardanoLogo" />
      </div>
    );
  }
}

InteractiveMap.defaultProps = {
  mapStep: 0,
  mapInitFirstStep: false,
  mapReset: false,
  onReset: null,
  controlsEnabled: true
};

InteractiveMap.propTypes = {
  mapStep: PropTypes.number,
  mapInitFirstStep: PropTypes.bool,
  mapReset: PropTypes.bool,
  onReset: PropTypes.func,
  controlsEnabled: PropTypes.bool
};

export default InteractiveMap;
