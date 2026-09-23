// @tg version=v0.0.1
// @tg title=`home API`
// @tg description=`A service which provide home API`
// @tg servers=`http://home-server:9000`
//
//go:generate tg transport --services . --out ../internal/transport
//go:generate tg swagger --services . --outFile ../api/swagger.yaml
package contracts
